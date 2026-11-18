/*
 * LECTAV — pedagogical math-to-speech conversion for local FOSS narration.
 * Original Markdown/LaTeX PDFs are NEVER modified.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

/**
 * Deterministic offline normalization: no LLM, network, or OCR.
 * Converts common classroom LaTeX into Catalan speech. Unrecognized
 * constructs are left for teacher review in the preview.
 */
internal object MathSpeechNormalizer {
    private val ordinalSingular = mapOf(
        2 to "mig", 3 to "terç", 4 to "quart", 5 to "cinquè",
        6 to "sisè", 7 to "setè", 8 to "vuitè", 9 to "novè",
        10 to "desè", 11 to "onzè", 12 to "dotzè")
    private val ordinalPlural = mapOf(
        2 to "meitats", 3 to "terços", 4 to "quarts", 5 to "cinquens",
        6 to "sisens", 7 to "setens", 8 to "vuitens", 9 to "novens",
        10 to "desens", 11 to "onzens", 12 to "dotzens")

    private fun argument(text: String, begin: Int): Pair<String,Int>? {
        var i=begin
        while(i<text.length && text[i].isWhitespace()) i++
        if(i>=text.length) return null
        if(text[i]=='{') {
            val start=++i
            var depth=1
            while(i<text.length && depth>0) {
                if(text[i]=='{') depth++
                if(text[i]=='}') depth--
                i++
            }
            if(depth!=0) return null
            return text.substring(start,i-1) to i
        }
        // TeX allows single-character numerator/denominator: \frac34
        val start=i
        if(text[i]=='-' || text[i]=='−') i++
        if(i>=text.length) return null
        i++
        return text.substring(start,i) to i
    }

    private fun spokenFraction(numerator: String, denominator: String): String {
        val num=numerator.trim()
        val den=denominator.trim()
        val n=num.toIntOrNull()
        val d=den.toIntOrNull()
        if(n!=null && d!=null && d in 2..12 && n>=0) {
            val unit=if(n==1) ordinalSingular[d] else ordinalPlural[d]
            return num+" "+unit
        }
        return speakMath(num)+" sobre "+speakMath(den)
    }

    private fun fractions(input: String): String {
        val output=StringBuilder()
        var i=0
        while(i<input.length) {
            if(input.startsWith("\\frac",i)) {
                val first=argument(input,i+5)
                val second=first?.let { argument(input,it.second) }
                if(first!=null && second!=null) {
                    output.append(" ")
                    output.append(spokenFraction(first.first,second.first))
                    output.append(" ")
                    i=second.second
                    continue
                }
            }
            output.append(input[i])
            i++
        }
        return output.toString()
    }

    fun speakMath(input: String): String {
        var text=fractions(input)
        text=text
            .replace("\\left","")
            .replace("\\right","")
            .replace("\\qquad","; ")
            .replace("\\quad","; ")
            .replace("\\cdot"," multiplicat per ")
            .replace("\\times"," multiplicat per ")
            .replace("\\div"," dividit per ")
            .replace("\\pm"," més o menys ")
            .replace("\\geq"," més gran o igual que ")
            .replace("\\leq"," més petit o igual que ")
            .replace("\\neq"," diferent de ")
            .replace(Regex("""\^\{2\}|\^2""")," al quadrat ")
            .replace(Regex("""\^\{3\}|\^3""")," al cub ")
            .replace("\\%"," per cent ")
            .replace("\\\\","; ")
            .replace(Regex("""\\[A-Za-z]+""")," ")
            .replace('−','-')
            .replace("="," igual a ")
            .replace("+"," més ")
            .replace("-"," menys ")
            .replace("*"," multiplicat per ")
            .replace(":"," dividit per ")
            .replace("/"," dividit per ")
            .replace("("," obre parèntesi ")
            .replace(")"," tanca parèntesi ")
            .replace("{"," ").replace("}"," ")
            .replace(Regex("""\s+""")," ")
        return text.trim()
    }

    fun normalize(text: String): String {
        // Apply maths *before* interpreting Markdown: preserve LaTeX grouping.
        var oral=text
            .replace(Regex("""(?s)\\\[(.*?)\\\]""")) { " "+speakMath(it.groupValues[1])+". " }
            .replace(Regex("""(?s)\\\((.*?)\\\)""")) { " "+speakMath(it.groupValues[1])+" " }
        // Standalone TeX math in an older extracted PDF without delimiters.
        if("\\frac" in oral || "\\cdot" in oral) {
            oral=oral.replace(Regex("""\\frac(?:\{[^{}]+\}|.)(?:\{[^{}]+\}|.)""")) {
                speakMath(it.value)
            }.replace("\\cdot"," multiplicat per ")
        }
        oral=oral.lines().filterNot { line ->
            line.trim().matches(Regex("""\|?[\s:|-]{3,}\|?"""))
        }.joinToString("\n")
            .replace(Regex("""(?m)^\s{0,3}#{1,6}\s*"""),"")
            .replace(Regex("""\*\*|__|[*]"""),"")
            .replace(Regex("""(?m)^\s*[-•]\s+"""),"")
            .replace("|","; ")
            .replace(Regex("""(?m)^\s*\d+\.\s*""")) { it.value.trim()+" " }
            .replace(Regex("""\s+""")," ")
            .trim()
        return oral
    }
}
