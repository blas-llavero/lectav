/*
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import org.junit.Assert.*
import org.junit.Test

class AvatarCaptionsTest {
    @Test fun coversWholeAudioWithoutGaps() {
        val cues = AvatarCaptions.fromText(
            "Hola a tothom. Avui aprendrem a configurar una xarxa segura.",
            12_000_000L, 30)
        assertTrue(cues.size >= 2)
        assertEquals(0L, cues.first().startUs)
        assertEquals(12_000_000L, cues.last().endUs)
        cues.zipWithNext().forEach { (a,b) -> assertEquals(a.endUs,b.startUs) }
        assertTrue(cues.all { it.endUs >= it.startUs && it.text.length <= 30 })
    }

    @Test fun handlesEmptyScript() {
        assertTrue(AvatarCaptions.fromText("  ", 1_000_000L).isEmpty())
    }

    @Test fun timelineIsDeterministic() {
        val a=AvatarCaptions.fromText("Una frase. Una altra.", 5_000_000L)
        assertEquals(a, AvatarCaptions.fromText("Una frase. Una altra.", 5_000_000L))
    }

    @Test fun speakingDependsOnAmplitude() {
        assertFalse(AvatarCaptions.mouthOpen(0))
        assertFalse(AvatarCaptions.mouthOpen(1800))
        assertTrue(AvatarCaptions.mouthOpen(-2500))
        assertTrue(AvatarCaptions.mouthOpen(-32768))
    }

    @Test fun rejectsInvalidInput() {
        assertThrows(IllegalArgumentException::class.java) {
            AvatarCaptions.fromText("Hola",0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AvatarCaptions.fromText("una_paraula_que_es_massa_llarga",1_000_000L,20)
        }
    }
}
