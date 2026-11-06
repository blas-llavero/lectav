/*
 * LECTAV — teacher-editable syllabus and local learner progress.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.learningStore by preferencesDataStore("lectav_learning_plan")

internal data class TopicPlan(
    val id: String,
    val title: String,
    val sessions: Int,
    val source: String,
    val completedSessions: Set<Int> = emptySet(),
    val trimester: Int = 1,
    val sessionTitles: List<String> = emptyList()
) {
    fun sessionTitle(number: Int): String =
        sessionTitles.getOrNull(number - 1)?.takeIf { it.isNotBlank() }
            ?: "Sessió ${number} · Contingut pendent de concretar"
}

// The decree defines competences/sabers, NOT one mandated first topic.
// These are proposed teacher-editable didactic units, never an official order.
// No preloaded, subject-specific curricular index. Each course and
// trimester can receive an explicitly approved AI-generated proposal.
internal fun starterTopicsFor(course: SavedCourse): List<TopicPlan> = emptyList()

internal data class CoursePlan(
    val courseKey: String,
    val topics: List<TopicPlan>
)

private fun decodePlans(raw: String): List<CoursePlan> = runCatching {
    val array = JSONArray(raw)
    buildList {
        for (i in 0 until minOf(100, array.length())) {
            val entry = array.optJSONObject(i) ?: continue
            val key = entry.optString("key")
            if (key.isBlank()) continue
            val topicsArray = entry.optJSONArray("topics") ?: JSONArray()
            val topics = buildList {
                for (j in 0 until minOf(100, topicsArray.length())) {
                    val t = topicsArray.optJSONObject(j) ?: continue
                    val id = t.optString("id")
                    val title = t.optString("title").take(150)
                    val sessions = t.optInt("sessions", 1).coerceIn(1, 60)
                    if (id.isBlank() || title.isBlank()) continue
                    val completed = t.optJSONArray("done") ?: JSONArray()
                    val done = (0 until completed.length())
                        .map { completed.optInt(it) }
                        .filter { it in 1..sessions }.toSet()
                    val lessonArray = t.optJSONArray("lessons") ?: JSONArray()
                    val lessons = (0 until minOf(sessions,lessonArray.length()))
                        .map { n -> lessonArray.optString(n).take(150) }
                    add(TopicPlan(
                        id, title, sessions, t.optString("source","teacher"),
                        done, t.optInt("trimester",1).coerceIn(1,3), lessons
                    ))
                }
            }
            add(CoursePlan(key, topics))
        }
    }
}.getOrDefault(emptyList())

private fun encodePlans(plans: List<CoursePlan>): String {
    val arr = JSONArray()
    plans.forEach { plan ->
        val topics = JSONArray()
        plan.topics.forEach { topic ->
            topics.put(JSONObject().apply {
                put("id", topic.id)
                put("title", topic.title)
                put("sessions", topic.sessions)
                put("source", topic.source)
                put("trimester",topic.trimester)
                put("lessons",JSONArray().apply { topic.sessionTitles.forEach { put(it) } })
                put("done", JSONArray().apply { topic.completedSessions.sorted().forEach { put(it) } })
            })
        }
        arr.put(JSONObject().put("key", plan.courseKey).put("topics", topics))
    }
    return arr.toString()
}

internal class CoursePlanningRepository(context: Context) {
    private val store = context.applicationContext.learningStore
    private val key = stringPreferencesKey("plans_v1")
    val plans = store.data.map { decodePlans(it[key] ?: "[]") }

    suspend fun snapshotRaw(): String =
        encodePlans(plans.first())

    suspend fun restoreBackup(raw: String) {
        require(raw.length <= 350_000) { "El pla curricular supera el límit." }
        val valid = decodePlans(raw)
        require(raw.trim() == "[]" || valid.isNotEmpty()) {
            "El pla curricular és invàlid."
        }
        store.edit { it[key] = encodePlans(valid) }
    }

    suspend fun initialize(course: SavedCourse): Boolean {
        val defaults = starterTopicsFor(course)
        if (defaults.isEmpty()) return false
        store.edit { prefs ->
            val existing = decodePlans(prefs[key] ?: "[]")
            if (existing.none { it.courseKey == courseKey(course) })
                prefs[key] = encodePlans(existing + CoursePlan(courseKey(course), defaults))
        }
        return true
    }

    /**
     * Explicit teacher approval replaces a course programme with the reviewed
     * AI proposal. Existing completed-session marks are intentionally reset;
     * callers must show an explicit replacement confirmation in the UI.
     */
    suspend fun saveReviewedProposal(course: SavedCourse, approved: List<TopicPlan>) {
        require(course.role == "teacher") {
            "Només el professor pot modificar la programació compartida."
        }
        require(approved.size in 1..16 && approved.all {
            it.title.isNotBlank() && it.title.length <= 150 &&
                it.sessions in 1..60
        }) { "La proposta de temari és incompleta." }
        store.edit { prefs ->
            val old = decodePlans(prefs[key] ?: "[]")
            val newPlan = CoursePlan(courseKey(course), approved)
            prefs[key] = encodePlans(old.filterNot {
                it.courseKey == newPlan.courseKey
            } + newPlan)
        }
    }

    /**
     * Save only the selected trimester; another trimester remains intact.
     * Existing progress within the replaced trimester is reset only after
     * the user has confirmed replacement in the visible interface.
     */
    suspend fun saveTrimesterProposal(
        course: SavedCourse,
        trimester: Int,
        proposed: List<TopicPlan>
    ) {
        require(course.role == "teacher")
        require(trimester in 1..3)
        require(proposed.size in 2..8 && proposed.all { topic ->
            topic.trimester == trimester &&
                topic.title.length in 3..150 &&
                topic.sessions in 1..30 &&
                topic.sessionTitles.size == topic.sessions &&
                topic.sessionTitles.all { it.length in 3..150 }
        })
        require(proposed.sumOf { it.sessions } in 6..48) {
            "El trimestre ha de tenir entre 6 i 48 sessions."
        }
        store.edit { prefs ->
            val existing = decodePlans(prefs[key] ?: "[]")
            val index = existing.indexOfFirst { it.courseKey == courseKey(course) }
            val old = existing.getOrNull(index)
            val combined = old?.topics.orEmpty().filterNot { it.trimester == trimester } +
                proposed.mapIndexed { index, t ->
                    t.copy(
                        id = "t${trimester}-u${index+1}",
                        completedSessions = emptySet(),
                        source = "ai-reviewed"
                    )
                }
            val ordered = combined.sortedWith(
                compareBy<TopicPlan> { it.trimester }.thenBy { it.id }
            )
            require(ordered.size <= 30)
            val updated = CoursePlan(courseKey(course), ordered)
            prefs[key] = encodePlans(existing.filterNot {
                it.courseKey == updated.courseKey
            } + updated)
        }
    }

    suspend fun saveSessionTitle(
        course: SavedCourse, topicId: String, session: Int, title: String
    ) {
        require(course.role == "teacher")
        val cleaned = title.trim().take(150)
        require(cleaned.length >= 3)
        store.edit { prefs ->
            prefs[key] = encodePlans(decodePlans(prefs[key] ?: "[]").map { plan ->
                if (plan.courseKey != courseKey(course)) plan else plan.copy(
                    topics = plan.topics.map { unit ->
                        if (unit.id != topicId || session !in 1..unit.sessions) unit
                        else {
                            val previous = (1..unit.sessions)
                                .map { unit.sessionTitle(it) }.toMutableList()
                            previous[session - 1] = cleaned
                            unit.copy(sessionTitles = previous, source = "teacher")
                        }
                    }
                )
            })
        }
    }

    suspend fun saveTopicTitle(course: SavedCourse, id: String, title: String) {
        val cleaned = title.trim().take(150)
        if (cleaned.isBlank()) return
        store.edit { prefs ->
            prefs[key] = encodePlans(decodePlans(prefs[key] ?: "[]").map { plan ->
                if (plan.courseKey == courseKey(course)) plan.copy(
                    topics = plan.topics.map { t ->
                        if (t.id == id) t.copy(title = cleaned, source = "teacher") else t
                    }
                ) else plan
            })
        }
    }

    suspend fun setSessionDone(course: SavedCourse, topicId: String, session: Int, done: Boolean) {
        store.edit { prefs ->
            prefs[key] = encodePlans(decodePlans(prefs[key] ?: "[]").map { plan ->
                if (plan.courseKey == courseKey(course)) plan.copy(
                    topics = plan.topics.map { t ->
                        if (t.id == topicId && session in 1..t.sessions) t.copy(
                            completedSessions = if (done) t.completedSessions + session
                                else t.completedSessions - session
                        ) else t
                    }
                ) else plan
            })
        }
    }
}
