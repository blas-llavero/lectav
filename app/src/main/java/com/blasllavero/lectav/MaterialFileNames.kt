/*
 * LECTAV — filenames shared by lesson PDFs, avatar videos and Save As.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

internal object MaterialFileNames {
    private fun typeLabel(type: String): String = when(type) {
        "lesson" -> "Teoria"
        "exercise" -> "Exercicis"
        "solutions" -> "Solucions"
        "video" -> "Avatar"
        else -> "Material"
    }

    private val sessionPrefix = Regex("""(?i)^Sessió\s+\d+\s*[-–—]\s*.+""")
    private val oldPrefix = Regex(
        """(?i)^(LECTAV|Temari|Teoria|Exercicis|Solucions|Vídeo|Video|Avatar|Material)\s*[-–—]\s*"""
    )
    private val oldSuffix = Regex(
        """(?i)\s*[-–—]\s*(Teoria|Exercicis|Solucions|Avatar)$"""
    )

    private fun safeStem(name: String): String = name.trim()
        .replace(Regex("""(?i)\.(pdf|mp4)$"""), "")
        .replace(oldPrefix, "")
        // FAT/SD cards and Windows disallow literal ':', among other chars.
        .replace(Regex("""\s*:\s*"""), " - ")
        .replace(Regex("""[\\/*?"<>|\u0000-\u001F]"""), "_")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', '–', '—')
        .take(124).ifBlank { "Document" }

    fun forDraft(topic: String, type: String): String =
        forExport(topic, type)

    fun avatarSession(session: String): String =
        forExport(session, "video")

    fun forExport(original: String, type: String): String {
        val extension = if(type == "video") ".mp4" else ".pdf"
        val base = safeStem(original)
        // Canonical session filenames have the *type at the end*.
        // Idempotent even when re-exporting previous LECTAV documents.
        val withoutSuffix = base.replace(oldSuffix, "").trim()
        val label = typeLabel(type)
        return if(sessionPrefix.matches(withoutSuffix)) {
            "$withoutSuffix - $label$extension"
        } else {
            // General documents that are not attached to a teaching session.
            "$label - $withoutSuffix$extension"
        }
    }
}
