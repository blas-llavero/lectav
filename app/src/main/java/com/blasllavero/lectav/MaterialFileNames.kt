/*
 * LECTAV — descriptive, stable file names for generated and exported material.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

internal object MaterialFileNames {
    private fun prefix(type: String) = when (type) {
        "lesson" -> "Temari"
        "exercise" -> "Exercicis"
        "solutions" -> "Solucions"
        "video" -> "Vídeo"
        else -> "Material"
    }

    fun forDraft(topic: String, type: String): String =
        forExport("LECTAV - " + topic.trim().take(90) + ".pdf", type)

    fun forExport(original: String, type: String): String {
        val extension = if (type == "video") ".mp4" else ".pdf"
        val name = original.trim()
            .replace(Regex("""(?i)\.(pdf|mp4)$"""), "")
            .replace(Regex("""(?i)^LECTAV\s*[-–—]\s*"""), "")
            .replace(Regex("""(?i)^(Temari|Exercicis|Solucions|Vídeo|Video|Material)\s*[-–—:]\s*"""), "")
            .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
            .trim().take(105).ifBlank { "Document" }
        return "${prefix(type)} - $name$extension"
    }
}
