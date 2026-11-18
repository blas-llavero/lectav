package com.blasllavero.lectav

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathSpeechNormalizerTest {
    @Test fun compactFractionsAndOperations() {
        val txt=MathSpeechNormalizer.normalize(
            """\[\frac34-\frac16=\frac9{12}-\frac2{12}=\frac7{12}\]""")
        assertTrue(txt.contains("3 quarts"))
        assertTrue(txt.contains("1 sisè"))
        assertTrue(txt.contains("7 dotzens"))
        assertFalse(txt.contains("\\frac"))
    }

    @Test fun negativesParenthesesAndMultiplication() {
        val text=MathSpeechNormalizer.normalize(
            """\((-6)\cdot(-3):(-2)\) i \(3-(-5)=3+5=8\)""")
        assertTrue(text.contains("multiplicat per"))
        assertTrue(text.contains("dividit per"))
        assertTrue(text.contains("obre parèntesi"))
        assertTrue(text.contains("menys"))
    }

    @Test fun educationalMarkdownTable() {
        val original="""# Sessió 1 — Diagnosi de càlcul amb enters i fraccions
| Núm. | Activitat |
|---|---|
| 1 | Suma d'enters: \(-12+7\) |
**Tancament:** explica la regla."""
        val oral=MathSpeechNormalizer.normalize(original)
        assertTrue(oral.contains("Diagnosi"))
        assertTrue(oral.contains("menys 12 més 7"))
        assertFalse(oral.contains("|---"))
    }

    @Test fun originalMathIsNotAltered() {
        val original="""\(\frac56:\frac{10}{9}=\frac34\)"""
        MathSpeechNormalizer.normalize(original)
        assertTrue(original.contains("\\frac"))
    }
}
