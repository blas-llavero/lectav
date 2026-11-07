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

    fun avatarSession(session: String): String {
        val safe=session.trim()
            .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
            .take(95).ifBlank { "Sessió" }
        return "Avatar - $safe.mp4"
    }

    fun forDraft(topic: String, type: String): String =
        forExport("LECTAV - " + topic.trim().take(90) + ".pdf", type)

    fun forExport(original: String, type: String): String {
        val extension = if (type == "video") ".mp4" else ".pdf"
        val name = original.trim()
            .replace(Regex("""(?i)\.(pdf|mp4)$"""), "")
            .replace(Regex("""(?i)^LECTAV\s*[-–—]\s*"""), "")
            .replace(Regex("""(?i)^(Temari|Exercicis|Solucions|Vídeo|Video|Avatar|Material)\s*[-–—:]\s*"""), "")
            .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
            .trim().take(105).ifBlank { "Document" }
        return if(type=="video" && original.trim().startsWith("Avatar - ",true))
            "Avatar - $name$extension"
        else "${prefix(type)} - $name$extension"
    }
}
