/*
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import org.junit.Assert.*
import org.junit.Test

class AvatarSegmentsTest {
    @Test fun oneHourProducesTwelveFiveMinuteSegments() {
        val parts = AvatarSegments.plan(3_600_000_000L)
        assertEquals(12, parts.size)
        assertEquals(0L, parts.first().startUs)
        assertEquals(3_600_000_000L, parts.last().endUs)
        assertTrue(parts.all { it.durationUs == 300_000_000L })
    }

    @Test fun partialTailHasNoGapsOrOverlaps() {
        val parts = AvatarSegments.plan(620_123_456L)
        assertEquals(3, parts.size)
        assertEquals(20_123_456L, parts.last().durationUs)
        parts.zipWithNext().forEach { (left,right) ->
            assertEquals(left.endUs, right.startUs)
            assertEquals(left.number + 1, right.number)
        }
    }

    @Test fun shortClipIsSingleSegment() {
        assertEquals(1, AvatarSegments.plan(100_000L).size)
        assertEquals(100_000L, AvatarSegments.plan(100_000L).first().endUs)
    }

    @Test fun alignsSampleBoundariesWithoutDuplicates() {
        val parts = AvatarSegments.plan(30_100_000L, 30)
        assertEquals(480_000L, AvatarSegments.sampleIndex(parts[1].startUs, 16000))
        assertEquals(
            AvatarSegments.sampleIndex(parts[0].endUs,16000),
            AvatarSegments.sampleIndex(parts[1].startUs,16000)
        )
    }

    @Test fun rejectsOverlongOrInvalidSegments() {
        assertThrows(IllegalArgumentException::class.java) {
            AvatarSegments.plan(3_600_000_001L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AvatarSegments.plan(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AvatarSegments.plan(1_000_000L, 601)
        }
    }
}
