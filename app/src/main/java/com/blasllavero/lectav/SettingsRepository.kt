/*
 * LECTAV — stored settings
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

// Single DataStore instance. Preserves the previous subtitle calibration.
private val Context.lectavDataStore by preferencesDataStore(
    name = "lectav_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "lectav_preferences"))
    }
)

// Stable (non-translated) identifiers. These represent saved course selections,
// not actual generated PDFs, course enrollments or learning records.
internal data class SavedCourse(
    val id: String,
    val role: String,
    val pathway: String,
    val course: String,
    val subject: String,
    val lastOpenedAt: Long
)

private fun bookmarkId(role: String, pathway: String, course: String, subject: String) =
    "$role|ES-CT|$pathway|$course|$subject"

private fun readSavedCourses(serialized: String): List<SavedCourse> = runCatching {
    val array = JSONArray(serialized)
    buildList {
        for (index in 0 until minOf(array.length(), 500)) {
            val item = array.optJSONObject(index) ?: continue
            val role = item.optString("role")
            val pathway = item.optString("pathway")
            val course = item.optString("course")
            val subject = item.optString("subject")
            if (role !in listOf("teacher", "student") ||
                pathway !in listOf("eso", "batx") ||
                subject.isBlank() || course.isBlank()
            ) continue
            add(
                SavedCourse(
                    id = bookmarkId(role, pathway, course, subject),
                    role = role,
                    pathway = pathway,
                    course = course,
                    subject = subject,
                    lastOpenedAt = item.optLong("lastOpenedAt", 0L)
                )
            )
        }
    }.distinctBy { it.id }
}.getOrDefault(emptyList())

private fun writeSavedCourses(items: List<SavedCourse>): String {
    val array = JSONArray()
    items.forEach { entry ->
        array.put(JSONObject().apply {
            put("role", entry.role)
            put("pathway", entry.pathway)
            put("course", entry.course)
            put("subject", entry.subject)
            put("lastOpenedAt", entry.lastOpenedAt)
        })
    }
    return array.toString()
}

internal data class SavedSettings(
    val countryCode: String = "ES",
    val regionCode: String = "ES-CT",
    val pathway: String = "",
    val course: String = "",
    val subject: String = "",
    val subtitleHeight: Float = 0.25f,
    val activeRole: String = "teacher",
    val savedCourses: List<SavedCourse> = emptyList()
)

internal class SettingsRepository(context: Context) {
    private val store = context.applicationContext.lectavDataStore
    private object Keys {
        val country = stringPreferencesKey("country_code")
        val region = stringPreferencesKey("region_code")
        val pathway = stringPreferencesKey("pathway")
        val course = stringPreferencesKey("course")
        val subject = stringPreferencesKey("subject")
        val subtitle = floatPreferencesKey("subtitle_height")
        val activeRole = stringPreferencesKey("active_role")
        val bookmarks = stringPreferencesKey("bookmarks_json_v1")
    }
    val settings: Flow<SavedSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            SavedSettings(
                countryCode = p[Keys.country] ?: "ES",
                regionCode = p[Keys.region] ?: "ES-CT",
                pathway = p[Keys.pathway] ?: "",
                course = p[Keys.course] ?: "",
                subject = p[Keys.subject] ?: "",
                subtitleHeight = (p[Keys.subtitle] ?: 0.25f).coerceIn(0f, 1f),
                activeRole = (p[Keys.activeRole] ?: "teacher").let {
                    if (it == "student") "student" else "teacher"
                },
                savedCourses = readSavedCourses(p[Keys.bookmarks] ?: "[]")
            )
        }

    suspend fun snapshot(): SavedSettings = settings.first()

    suspend fun restoreBackup(value: SavedSettings) {
        require(value.activeRole in setOf("teacher", "student"))
        require(value.savedCourses.size <= 500)
        store.edit { p ->
            p[Keys.country] = value.countryCode.take(12)
            p[Keys.region] = value.regionCode.take(24)
            p[Keys.pathway] = value.pathway.take(40)
            p[Keys.course] = value.course.take(40)
            p[Keys.subject] = value.subject.take(100)
            p[Keys.subtitle] = value.subtitleHeight.coerceIn(0f, 1f)
            p[Keys.activeRole] = value.activeRole
            p[Keys.bookmarks] = writeSavedCourses(value.savedCourses)
        }
    }

    suspend fun choosePathway(pathway: String) {
        store.edit {
            it[Keys.country] = "ES"
            it[Keys.region] = "ES-CT"
            it[Keys.pathway] = pathway
            it[Keys.course] = ""
            it[Keys.subject] = ""
        }
    }
    suspend fun chooseCourse(course: String) {
        store.edit {
            it[Keys.course] = course
            it[Keys.subject] = ""
        }
    }
    suspend fun chooseSubject(subject: String) {
        store.edit { it[Keys.subject] = subject }
    }
    suspend fun saveSubtitleHeight(height: Float) {
        store.edit { it[Keys.subtitle] = height.coerceIn(0f, 1f) }
    }
    suspend fun clearSelection() {
        store.edit {
            it[Keys.pathway] = ""
            it[Keys.course] = ""
            it[Keys.subject] = ""
        }
    }
    suspend fun setActiveRole(role: String) {
        require(role in listOf("teacher", "student"))
        store.edit { it[Keys.activeRole] = role }
    }

    suspend fun saveCurrentCourse() {
        store.edit { prefs ->
            val role = (prefs[Keys.activeRole] ?: "teacher").let {
                if (it == "student") "student" else "teacher"
            }
            val pathway = prefs[Keys.pathway] ?: ""
            val course = prefs[Keys.course] ?: ""
            val subject = prefs[Keys.subject] ?: ""
            if (pathway !in listOf("eso", "batx") ||
                course.isBlank() || subject.isBlank() ||
                catalogueSubjectsFor(course).none { it.id == subject }
            ) return@edit

            val id = bookmarkId(role, pathway, course, subject)
            val saved = readSavedCourses(prefs[Keys.bookmarks] ?: "[]")
            val updated = SavedCourse(
                id = id, role = role, pathway = pathway, course = course,
                subject = subject, lastOpenedAt = System.currentTimeMillis()
            )
            prefs[Keys.bookmarks] = writeSavedCourses(
                saved.filterNot { it.id == id } + updated
            )
        }
    }

    suspend fun openSavedCourse(id: String) {
        store.edit { prefs ->
            val saved = readSavedCourses(prefs[Keys.bookmarks] ?: "[]")
            val selected = saved.firstOrNull { it.id == id } ?: return@edit
            prefs[Keys.country] = "ES"
            prefs[Keys.region] = "ES-CT"
            prefs[Keys.pathway] = selected.pathway
            prefs[Keys.course] = selected.course
            prefs[Keys.subject] = selected.subject
            prefs[Keys.activeRole] = selected.role
            prefs[Keys.bookmarks] = writeSavedCourses(saved.map {
                if (it.id == id) it.copy(lastOpenedAt = System.currentTimeMillis())
                else it
            })
        }
    }

    suspend fun removeSavedCourse(id: String) {
        store.edit { prefs ->
            val list = readSavedCourses(prefs[Keys.bookmarks] ?: "[]")
            prefs[Keys.bookmarks] = writeSavedCourses(list.filterNot { it.id == id })
            // Deliberately keep the last selected subject: removing a bookmark
            // must not erase educational settings or future generated material.
        }
    }

}
