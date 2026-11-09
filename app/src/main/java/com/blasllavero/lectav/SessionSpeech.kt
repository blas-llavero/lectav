/*
 * LECTAV — private, on-device theory extraction and reviewed lesson scripts.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal object SessionSpeech {
    suspend fun extractPdf(context: Context, uri: String): String =
        withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            val input = context.contentResolver.openInputStream(Uri.parse(uri))
                ?: error("No es pot obrir el PDF de teoria.")
            input.use { source ->
                val buffer = ByteArray(8192)
                val output = ByteArrayOutputStream()
                while(true) {
                    val n = source.read(buffer)
                    if (n < 0) break
                    require(output.size()+n <= 12*1024*1024) {
                        "El PDF supera els 12 MB."
                    }
                    output.write(buffer,0,n)
                }
                PDDocument.load(output.toByteArray()).use { doc ->
                    require(doc.numberOfPages in 1..30) {
                        "El PDF supera les 30 pàgines."
                    }
                    PDFTextStripper().getText(doc)
                        .replace(Regex("""[ \t]{2,}"""), " ")
                        .trim()
                        .take(24000)
                        .also {
                            require(it.isNotBlank()) {
                                "El PDF no conté text seleccionable. Caldria OCR local."
                            }
                        }
                }
            }
        }

    // A two-speaker *draft*, not an AI-written pedagogical conversation.
    // The teacher should revise the script and correct mathematical notation.
    fun dialogueDraft(theory: String): String {
        val paragraphs = theory.split(Regex("""\n\s*\n"""))
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotEmpty() }
            .flatMap { it.chunked(420) }
            .take(36)
        return buildString {
            append("A: Avui treballarem els conceptes de la sessió.\n")
            paragraphs.forEachIndexed { index, part ->
                append(if(index % 2 == 0) "B: " else "A: ")
                append(part)
                append('\n')
            }
            append("A: Repassa els exemples i resol els exercicis per practicar.")
        }.take(24000)
    }
}
