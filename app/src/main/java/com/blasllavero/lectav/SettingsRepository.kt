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
import java.io.IOException

// Single DataStore instance. Preserves the previous subtitle calibration.
private val Context.lectavDataStore by preferencesDataStore(
    name = "lectav_settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "lectav_preferences"))
    }
)

internal data class SavedSettings(
    val countryCode: String = "ES",
    val regionCode: String = "ES-CT",
    val pathway: String = "",
    val course: String = "",
    val subject: String = "",
    val subtitleHeight: Float = 0.25f
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
                subtitleHeight = (p[Keys.subtitle] ?: 0.25f).coerceIn(0f, 1f)
            )
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
}
