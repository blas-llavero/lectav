/* Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later */
package com.blasllavero.lectav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PodcastDialogueTest {
    @Test fun parsesTwoSpeakersInOrder() {
        val turns = PodcastDialogue.parse("A: Què és una xarxa?\nB: Una connexió entre equips.")
        assertEquals(listOf('A', 'B'), turns.map { it.speaker })
        assertEquals("Què és una xarxa?", turns[0].text)
    }
    @Test fun toleratesWhitespaceAndBlankLines() {
        val turns = PodcastDialogue.parse("  A:   Benvinguts  \n\n B: Resposta ")
        assertEquals(2, turns.size)
        assertEquals("Benvinguts", turns[0].text)
    }
    @Test fun rejectsMalformedSpeech() {
        for (script in listOf("Hola!", "A: ", "C: Hola", "A: Hola\nB:", "A:Hola")) {
            assertThrows(IllegalArgumentException::class.java) { PodcastDialogue.parse(script) }
        }
    }
    @Test fun limitsInputAndTurns() {
        assertThrows(IllegalArgumentException::class.java) {
            PodcastDialogue.parse("A: 123456", maxCharacters = 5)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PodcastDialogue.parse("A: U\nB: Dos", maxTurns = 1)
        }
    }
}
