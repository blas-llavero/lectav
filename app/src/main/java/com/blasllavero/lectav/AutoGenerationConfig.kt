/*
 * LECTAV — persistent configuration for manual and automatic generation.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

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
        prefs(context).getString("homeMode", "automatic")?.takeIf {
            it in setOf("manual","automatic")
        } ?: "automatic"

    fun saveHomeMode(context: Context, value: String) {
        require(value in setOf("manual","automatic"))
        check(prefs(context).edit().putString("homeMode",value).commit())
    }

    /**
     * Prefer the strongest general-purpose model among models actually exposed
     * by the authenticated account. Never invent inaccessible model IDs.
     * Unknown models rank below known general-purpose ones.
     */
    fun strongestAvailableModel(models: List<ChatGPTModel>): ChatGPTModel? {
        fun score(m: ChatGPTModel): Int {
            val id = m.slug.lowercase(Locale.ROOT)
            val name = m.name.lowercase(Locale.ROOT)
            val label = "$id $name"
            if (listOf("embedding", "audio", "realtime", "transcribe", "whisper",
                    "tts", "image", "moderation", "search", "mini", "nano")
                    .any { it in label }) return -100
            val match = Regex("""gpt[-_ ]?(\d+)(?:[.\-_ ](\d+))?""")
                .find(id) ?: Regex("""gpt[-_ ]?(\d+)(?:[.\-_ ](\d+))?""")
                .find(name)
            if (match != null) {
                val major=match.groupValues[1].toIntOrNull() ?: 0
                val minor=match.groupValues[2].toIntOrNull() ?: 0
                return major*1000+minor*100 +
                    (if("pro" in label) 90 else 0) +
                    (if("thinking" in label || "reasoning" in label) 40 else 0)
            }
            return if("pro" in label) 100 else 0
        }
        return models.withIndex().maxWithOrNull(
            compareBy<IndexedValue<ChatGPTModel>> { score(it.value) }
                .thenBy { -it.index }
        )?.value
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

    /**
     * Persistent, revocable and account-scoped approval for content creation
     * with the signed-in ChatGPT account. Do not export or restore consent:
     * an installed account must explicitly authorize its own device.
     *
     * The opaque SHA-256 digest avoids storing an email address a second time.
     * The email itself comes from the authenticated account state.
     */
    private fun accountFingerprint(email: String): String {
        val normalized=email.trim().lowercase(Locale.ROOT)
        if(normalized.isBlank()) return ""
        val hash=MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun isChatGPTAuthorized(context: Context, email: String): Boolean {
        val account=accountFingerprint(email)
        return account.isNotBlank() &&
            prefs(context).getString("chatgptConsentAccount","") == account &&
            prefs(context).getBoolean("chatgptConsentGranted",false)
    }

    fun setChatGPTAuthorized(context: Context, email: String, authorized: Boolean) {
        val account=accountFingerprint(email)
        require(!authorized || account.isNotBlank()) {
            "Connecta un compte ChatGPT abans d'autoritzar-ne l'ús."
        }
        val editor=prefs(context).edit()
        if(authorized) {
            editor.putString("chatgptConsentAccount",account)
                .putBoolean("chatgptConsentGranted",true)
        } else {
            editor.remove("chatgptConsentAccount")
                .remove("chatgptConsentGranted")
        }
        check(editor.commit()) { "No s'ha desat l'autorització de ChatGPT." }
    }

    fun requireChatGPTAuthorized(context: Context, email: String) {
        check(isChatGPTAuthorized(context,email)) {
            "Activa a Configuració «He revisat la configuració d'ús i crèdits " +
                "del meu compte i autoritzo la creació de contingut amb ChatGPT»."
        }
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
        AppLanguageSupport.contentLanguage(context)

    fun lastProgress(context: Context): String =
        prefs(context).getString("autoLastProgress","").orEmpty()

    fun recordProgress(context: Context, value: String) {
        check(prefs(context).edit()
            .putString("autoLastProgress",value.take(400)).commit())
    }

    /**
     * Only non-secret UI preferences are exported. Account-scoped consent
     * is never backed up or restored, even if a different account is connected.
     */
    val backupKeys: List<String> = listOf(
        "language","kind","mode","endpoint","ollamaModel","chatgptModel",
        "homeMode","autoGlobalModel","autoMaxRequests","autoOverrides",
        "autoLastProgress"
    ) + materialTypes.map { "autoKind_$it" }
}
