/* LECTAV podcast turn validation. Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later */
package com.blasllavero.lectav

internal object PodcastDialogue {
    data class Turn(val speaker: Char, val text: String)

    fun parse(script: String, maxCharacters: Int = 8000, maxTurns: Int = 45): List<Turn> {
        require(maxCharacters > 0 && maxTurns > 0)
        require(script.length in 2..maxCharacters) { "El guió és massa curt o supera el límit." }
        val lines = script.lines().map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.isNotEmpty() && lines.size <= maxTurns) {
            "El podcast necessita entre 1 i $maxTurns intervencions."
        }
        return lines.map { line ->
            require(line.length >= 4 && line[0] in setOf('A', 'B') &&
                line[1] == ':' && line[2].isWhitespace() &&
                line.substring(3).isNotBlank()) {
                "Cada intervenció del podcast ha de començar amb A: o B:."
            }
            Turn(line[0], line.substring(3).trim())
        }
    }
}
