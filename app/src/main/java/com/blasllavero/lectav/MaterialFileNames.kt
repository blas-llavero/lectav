/*
 * LECTAV — consistent, portable filenames for session resources.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

internal object MaterialFileNames {
    private fun typeLabel(type: String): String = when (type) {
        "lesson" -> "Teoria"
        "exercise" -> "Exercicis"
        "solutions" -> "Solucions"
        "video" -> "Avatar"
        "audio" -> "Audio"
        "podcast" -> "Podcast"
        "transcript" -> "Transcripcio"
        else -> "Material"
    }

    private fun extension(type: String): String = when (type) {
        "video" -> ".mp4"
        "audio", "podcast" -> ".mp3"
        else -> ".pdf"
    }

    private val sessionPrefix = Regex("""(?i)^Sessió\s+\d+\s*[-–—]\s*.+""")
    private val oldPrefix = Regex(
        """(?i)^(LECTAV|Temari|Teoria|Exercicis|Solucions|Vídeo|Video|Avatar|Audio|Àudio|Podcast|Transcripció|Transcripcio|Material)\s*[-–—]\s*"""
    )
    private val oldSuffix = Regex(
        """(?i)\s*[-–—]\s*(Teoria|Exercicis|Solucions|Avatar|Audio|Àudio|Podcast|Transcripció|Transcripcio)$"""
    )
    private val canonicalSuffix = Regex(
        """(?i)^(U\d{2,}_S\d{2,}_.+?)_(Teoria|Exercicis|Solucions|Avatar|Audio|Podcast|Transcripcio)$"""
    )

    private fun safeStem(name: String): String = name.trim()
        .replace(Regex("""(?i)\.(pdf|mp4|mp3)$"""), "")
        .replace(oldPrefix, "")
        .replace(Regex("""\s*:\s*"""), " - ")
        .replace(Regex("""[\\/*?"<>|\u0000-\u001F]"""), "_")
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', '–', '—')
        .take(124).ifBlank { "Document" }

    /** Course-wide unit number, trimester-local visible session number. */
    fun sessionStem(unitNumber: Int, sessionNumber: Int, sessionTitle: String): String {
        require(unitNumber > 0 && sessionNumber > 0)
        val title = safeStem(sessionTitle)
            .replace(Regex("""[\s_]+"""), "_")
            .trim('_')
            .take(88)
            .trimEnd('_')
            .ifBlank { "Sessio" }
        return "U${unitNumber.toString().padStart(2, '0')}_S${sessionNumber.toString().padStart(2, '0')}_$title"
    }

    fun forSessionStem(stem: String, type: String): String =
        "${stem}_${typeLabel(type)}${extension(type)}"

    fun forSession(unitNumber: Int, sessionNumber: Int, title: String, type: String): String =
        forSessionStem(sessionStem(unitNumber, sessionNumber, title), type)

    /**
     * Regenerate the suggested export name from the current curriculum position.
     * Historical files are not renamed or overwritten; their saved session key
     * is used only to suggest a name when the user exports a new copy.
     */
    fun forMaterial(material: LocalMaterial, plan: CoursePlan?): String {
        val parts = material.sessionKey.split('|')
        if (parts.size == 3 && plan != null) {
            val trimester = parts[0].toIntOrNull()
            val localNumber = parts[2].toIntOrNull()
            val unitIndex = plan.topics.indexOfFirst {
                it.id == parts[1] && it.trimester == trimester
            }
            if (unitIndex >= 0 && localNumber != null && localNumber in 1..plan.topics[unitIndex].sessions) {
                val unit = plan.topics[unitIndex]
                val termUnits = plan.topics.filter { it.trimester == trimester }
                val termIndex = termUnits.indexOfFirst { it.id == unit.id }
                if (termIndex >= 0) {
                    val sessionNumber = termUnits.take(termIndex).sumOf { it.sessions } + localNumber
                    return forSession(unitIndex + 1, sessionNumber, unit.sessionTitle(localNumber), material.type)
                }
            }
        }
        return forExport(material.name, material.type)
    }

    fun forDraft(topic: String, type: String): String = forExport(topic, type)

    fun forExport(original: String, type: String): String {
        val ext = extension(type)
        val base = safeStem(original)
        val canonical = canonicalSuffix.matchEntire(base)
        if (canonical != null) {
            return "${canonical.groupValues[1]}_${typeLabel(type)}$ext"
        }
        val withoutSuffix = base.replace(oldSuffix, "").trim()
        val label = typeLabel(type)
        return if (sessionPrefix.matches(withoutSuffix)) {
            "$withoutSuffix - $label$ext"
        } else {
            "$label - $withoutSuffix$ext"
        }
    }
}
