/*
 * LECTAV — one shared language decision for app and new materials.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

internal object AppLanguageSupport {
    private val supported = mapOf(
        "en" to "English",
        "ca" to "Català",
        "es" to "Español",
        "fr" to "Français",
        "pt" to "Português",
        "de" to "Deutsch",
        "ar" to "العربية",
        "hi" to "हिन्दी",
        "zh" to "中文",
        "ja" to "日本語"
    )

    fun languageCode(context: Context): String {
        val chosen = AppCompatDelegate.getApplicationLocales()
            .toLanguageTags().takeIf { it.isNotBlank() }
            ?.let { Locale.forLanguageTag(it.substringBefore(",")).language }
        val system = context.resources.configuration.locales
            .takeIf { !it.isEmpty }?.get(0)?.language
        val code = (chosen ?: system ?: "en").lowercase(Locale.ROOT)
        return if (code in supported) code else "en"
    }

    fun contentLanguage(context: Context): String =
        supported.getValue(languageCode(context))
}
