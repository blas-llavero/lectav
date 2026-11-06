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
    val term: Int = 1,
    val sessionTitles: List<String> = emptyList()
)

// No predefined thematic content is bundled in new releases. Existing
// historical plans in DataStore remain readable after an upgrade.
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
                    val quarter = t.optInt("term",1).coerceIn(1,3)
                    val titlesJson = t.optJSONArray("sessionTitles") ?: JSONArray()
                    val titles = (0 until minOf(sessions,titlesJson.length())).map {
                        titlesJson.optString(it).trim().take(180)
                    }
                    add(TopicPlan(id, title, sessions, t.optString("source","teacher"),
                        done, quarter, titles))
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
                put("term", topic.term)
                put("sessionTitles", JSONArray().apply {
                    topic.sessionTitles.take(60).forEach { put(it) }
                })
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
     * Save a reviewed ChatGPT proposal for ONE trimester. Previous
     * trimesters, including their completed-session progress, are retained.
     * Replacing the same trimester resets only those sessions.
     */
    suspend fun saveTermPlan(
        course: SavedCourse, term: Int, topics: List<TopicPlan>
    ) {
        require(course.role == "teacher")
        require(term in 1..3)
        require(topics.size in 1..8)
        require(topics.all {
            it.term == term && it.sessions in 1..40 &&
                it.sessionTitles.size == it.sessions &&
                it.sessionTitles.all { title -> title.trim().length in 3..180 } &&
                it.title.trim().length in 3..150
        }) { "Cal revisar els títols i les sessions abans de desar." }
        store.edit { prefs ->
            val saved = decodePlans(prefs[key] ?: "[]")
            val id = courseKey(course)
            val existing = saved.firstOrNull { it.courseKey == id }
            val retained = existing?.topics.orEmpty().filter { it.term != term }
            val next = CoursePlan(id, (retained + topics).sortedBy { it.term })
            prefs[key] = encodePlans(
                saved.filterNot { it.courseKey == id } + next
            )
        }
    }

    suspend fun saveSessionTitle(
        course: SavedCourse, topicId: String, number: Int, name: String
    ) {
        require(course.role == "teacher")
        val clean = name.trim().take(180)
        if (clean.length < 3) return
        store.edit { p ->
            p[key] = encodePlans(decodePlans(p[key] ?: "[]").map { plan ->
                if (plan.courseKey == courseKey(course)) plan.copy(
                    topics = plan.topics.map { topic ->
                        if (topic.id == topicId && number in 1..topic.sessions &&
                            topic.sessionTitles.size == topic.sessions
                        ) topic.copy(sessionTitles = topic.sessionTitles.mapIndexed {
                            index, title -> if (index + 1 == number) clean else title
                        }, source = "teacher") else topic
                    }
                ) else plan
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
