/*
 * LECTAV — bounded, deterministic segments for long avatar lessons.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

/**
 * Pure planning only: no long-form MP4 renderer is enabled by this object.
 * Keeps time ranges half-open so adjacent segments do not repeat audio samples.
 */
internal object AvatarSegments {
    const val DEFAULT_SEGMENT_SECONDS = 300
    const val MAX_LESSON_SECONDS = 3600

    data class Part(
        val number: Int,
        val startUs: Long,
        val endUs: Long
    ) {
        val durationUs: Long get() = endUs - startUs
    }

    fun plan(
        durationUs: Long,
        segmentSeconds: Int = DEFAULT_SEGMENT_SECONDS
    ): List<Part> {
        require(durationUs in 1..MAX_LESSON_SECONDS * 1_000_000L) {
            "Una lliçó d'avatar no pot superar els 60 minuts."
        }
        require(segmentSeconds in 30..600) {
            "Els fragments han de durar entre 30 segons i 10 minuts."
        }
        val slice = segmentSeconds * 1_000_000L
        val count = ((durationUs - 1L) / slice + 1L).toInt()
        require(count <= 120)
        return (0 until count).map { index ->
            val start = index * slice
            Part(index + 1, start, minOf(durationUs, start + slice))
        }
    }

    /** Align a timeline boundary to a 16-bit PCM sample (no duplicated samples). */
    fun sampleIndex(startUs: Long, sampleRate: Int): Long {
        require(startUs >= 0 && sampleRate in 8000..48000)
        return startUs * sampleRate / 1_000_000L
    }
}
