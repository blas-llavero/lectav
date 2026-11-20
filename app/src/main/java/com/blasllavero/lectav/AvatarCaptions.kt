/*
 * LECTAV — deterministic subtitle and animation timing for offline avatars.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

/**
 * Pure Kotlin timeline for the future AVC/AAC MP4 renderer.
 * Captions are approximate phrase timing, NOT speech-to-text alignment.
 * Timestamps use microseconds, as required by Android MediaCodec.
 */
internal object AvatarCaptions {
    data class Cue(val startUs: Long, val endUs: Long, val text: String)

    fun fromText(script: String, durationUs: Long, maxCueChars: Int = 70): List<Cue> {
        require(durationUs > 0)
        require(maxCueChars in 20..200)
        val words = script.trim().split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val groups = mutableListOf<String>()
        var part = ""
        for (word in words) {
            require(word.length <= maxCueChars) { "A word exceeds the avatar subtitle limit." }
            if (part.isNotBlank() && part.length + word.length + 1 > maxCueChars) {
                groups.add(part)
                part = ""
            }
            part = if (part.isBlank()) word else "$part $word"
            if (word.endsWith('.') || word.endsWith('!') || word.endsWith('?')) {
                groups.add(part)
                part = ""
            }
        }
        if (part.isNotBlank()) groups.add(part)
        val weight = groups.map { it.length.toLong().coerceAtLeast(1L) }
        val total = weight.sum()
        var elapsed = 0L
        var consumed = 0L
        return groups.mapIndexed { index, cue ->
            consumed += weight[index]
            val end = if (index == groups.lastIndex) durationUs
                else (durationUs * consumed / total).coerceIn(elapsed, durationUs)
            Cue(elapsed, end, cue).also { elapsed = end }
        }
    }

    /** Basic amplitude-based mouth movement, not phoneme-accurate lip sync. */
    fun mouthOpen(peakPcm16: Int, threshold: Int = 1800): Boolean {
        require(threshold in 0..32767)
        return kotlin.math.abs(peakPcm16.toLong()) > threshold
    }
}
