/*
 * LECTAV — persistent configuration for manual and automatic generation.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import org.json.JSONObject

/**
 * Config values contain no OAuth access/refresh token. The encrypted backup
 * stores these user preferences, but never a valid ChatGPT login session.
 */
internal object AutoGenerationConfig {
    private const val PREFS = "lectav_generator"
    val materialTypes = listOf("lesson", "exercise", "solutions", "video",
        "audio", "podcast", "transcript")
    val automaticPdfTypes = listOf("lesson", "exercise", "solutions", "transcript")

    fun title(kind: String): String = when(kind) {
        "lesson" -> "Teoria"
        "exercise" -> "Exercicis"
        "solutions" -> "Solucions"
        "video" -> "Avatar MP4"
        "audio" -> "Àudio"
        "podcast" -> "Podcast"
        "transcript" -> "Transcripció"
        else -> kind
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun homeMode(context: Context): String =
        prefs(context).getString("homeMode", "manual")?.takeIf {
            it in setOf("manual","automatic")
        } ?: "manual"

    fun saveHomeMode(context: Context, value: String) {
        require(value in setOf("manual","automatic"))
        check(prefs(context).edit().putString("homeMode",value).commit())
    }

    fun globalModel(context: Context): String =
        prefs(context).getString("autoGlobalModel","").orEmpty()

    fun setGlobalModel(context: Context, slug: String) {
        check(prefs(context).edit().putString("autoGlobalModel",slug.take(120)).commit())
    }

    fun kindModel(context: Context, kind: String): String {
        require(kind in materialTypes)
        return prefs(context).getString("autoKind_$kind","").orEmpty()
    }

    fun setKindModel(context: Context, kind: String, slug: String) {
        require(kind in materialTypes)
        check(prefs(context).edit()
            .putString("autoKind_$kind",slug.take(120)).commit())
    }

    // A course+session-specific dropdown overrides both type and global model.
    // Keys are stable IDs from the already saved course planner.
    private fun overrideKey(courseKey: String, sessionKey: String, kind: String): String =
        "$courseKey|$sessionKey|$kind"

    private fun overrides(context: Context) = runCatching {
        JSONObject(prefs(context).getString("autoOverrides","{}") ?: "{}")
    }.getOrElse { JSONObject() }

    fun explicitOverride(context: Context, courseKey: String,
                         sessionKey: String, kind: String): String =
        overrides(context).optString(overrideKey(courseKey,sessionKey,kind))

    fun setOverride(context: Context, courseKey: String, sessionKey: String,
                    kind: String, slug: String) {
        require(kind in materialTypes && sessionKey.length in 1..150 &&
            courseKey.length in 1..200)
        val data=overrides(context)
        val key=overrideKey(courseKey,sessionKey,kind)
        if(slug.isBlank()) data.remove(key)
        else data.put(key,slug.take(120))
        require(data.length() <= 3500 && data.toString().length <= 400_000) {
            "Massa configuracions de models. Elimina excepcions antigues."
        }
        check(prefs(context).edit().putString("autoOverrides",data.toString()).commit())
    }

    fun resolvedModel(context: Context, courseKey: String,
                      sessionKey: String, kind: String): String {
        if(kind !in materialTypes) return globalModel(context)
        return explicitOverride(context,courseKey,sessionKey,kind)
            .ifBlank { kindModel(context,kind) }
            .ifBlank { globalModel(context) }
    }

    fun automaticAuthorized(context: Context): Boolean =
        prefs(context).getString("autoAuthorized","false") == "true"

    fun setAutomaticAuthorized(context: Context, authorized: Boolean) {
        check(prefs(context).edit()
            .putString("autoAuthorized",authorized.toString()).commit())
    }

    fun maxRequests(context: Context): Int =
        (prefs(context).getString("autoMaxRequests","20")?.toIntOrNull() ?: 20)
            .coerceIn(1,500)

    fun setMaxRequests(context: Context, n: Int) {
        require(n in 1..500)
        check(prefs(context).edit()
            .putString("autoMaxRequests",n.toString()).commit())
    }

    fun generationLanguage(context: Context): String =
        prefs(context).getString("language","Català").orEmpty()
            .ifBlank { "Català" }

    fun lastProgress(context: Context): String =
        prefs(context).getString("autoLastProgress","").orEmpty()

    fun recordProgress(context: Context, value: String) {
        check(prefs(context).edit()
            .putString("autoLastProgress",value.take(400)).commit())
    }

    /**
     * Non-secret preferences included in .lectavbak. 'autoAuthorized' is
     * deliberately NOT restored as true; re-authorize bulk billing/usage
     * when using the backup on a new installation.
     */
    val backupKeys: List<String> = listOf(
        "language","kind","mode","endpoint","ollamaModel","chatgptModel",
        "homeMode","autoGlobalModel","autoMaxRequests","autoOverrides",
        "autoLastProgress"
    ) + materialTypes.map { "autoKind_$it" }
}
