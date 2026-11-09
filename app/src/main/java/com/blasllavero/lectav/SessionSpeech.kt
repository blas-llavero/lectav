/*
 * LECTAV — private, on-device theory extraction and reviewed lesson scripts.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal object SessionSpeech {

    // PDF size != speech size: images can dominate the input without adding
    // readable text. Stage the PDF on private disk to avoid 17+ MB byte arrays.
    // The separate 8k character cap is the current synthesis/UI safety limit.
    private const val MAX_PDF_BYTES = 80L * 1024 * 1024
    const val MAX_SCRIPT_CHARS = 8000
    private const val MAX_PAGES = 250

    data class PdfExtract(
        val text: String,
        val pages: Int,
        val startPage: Int,
        val endPage: Int,
        val wasTruncated: Boolean
    )

    /**
     * PDFBox's disk-backed parser holds decoded streams outside the heap.
     * Uses no network or OCR; source PDFs are never modified.
     * pageEnd=0 means 'through the final page'.
     */
    suspend fun extractPdf(
        context: Context,
        uri: String,
        pageStart: Int = 1,
        pageEnd: Int = 0
    ): PdfExtract = withContext(Dispatchers.IO) {
        require(pageStart in 1..MAX_PAGES) { "La pàgina inicial ha d'estar entre 1 i $MAX_PAGES." }
        require(pageEnd == 0 || pageEnd in pageStart..MAX_PAGES) {
            "La pàgina final ha de ser zero (totes) o igual/superior a la inicial."
        }
        PDFBoxResourceLoader.init(context.applicationContext)
        val stagedPdf = File(context.cacheDir, "theory_${UUID.randomUUID()}.pdf")
        try {
            val resolver = context.contentResolver
            val input = resolver.openInputStream(Uri.parse(uri))
                ?: error("No es pot obrir el PDF de teoria.")
            input.use { source ->
                stagedPdf.outputStream().buffered().use { target ->
                    val buffer = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= MAX_PDF_BYTES) {
                            "PDF superior a 80 MB; cal treballar amb un fitxer més petit."
                        }
                        target.write(buffer, 0, count)
                    }
                    require(size >= 5) { "El PDF és buit." }
                }
            }
            stagedPdf.inputStream().use { headerStream ->
                val header = ByteArray(4)
                require(headerStream.read(header) == 4 &&
                    String(header, Charsets.US_ASCII) == "%PDF") {
                    "El document seleccionat no és un PDF vàlid."
                }
            }
            PDDocument.load(stagedPdf, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                val pages = doc.numberOfPages
                require(pages in 1..MAX_PAGES) {
                    "El PDF té $pages pàgines; actualment s'admeten fins a $MAX_PAGES."
                }
                require(pageStart <= pages) {
                    "La pàgina inicial $pageStart supera les $pages pàgines del PDF."
                }
                val last = if (pageEnd == 0) pages else minOf(pageEnd, pages)
                val buffer = StringBuilder()
                var truncated = false
                for (page in pageStart..last) {
                    coroutineContext.ensureActive()
                    val pageText = PDFTextStripper().apply {
                        startPage = page
                        endPage = page
                    }.getText(doc)
                    val normalized = pageText.replace(Regex("""[ \\t]{2,}"""), " ").trim()
                    if (normalized.isEmpty()) continue
                    val room = MAX_SCRIPT_CHARS - buffer.length
                    if (room <= 0) {
                        truncated = true
                        break
                    }
                    val separator = if (buffer.isNotEmpty()) "\\n\\n" else ""
                    val addition = separator + normalized
                    if (addition.length > room) {
                        buffer.append(addition.take(room))
                        truncated = true
                        break
                    }
                    buffer.append(addition)
                }
                val extracted = buffer.toString().trim()
                require(extracted.isNotBlank()) {
                    "No hi ha text seleccionable en les pàgines indicades. Pot caldre OCR local."
                }
                PdfExtract(extracted, pages, pageStart, last, truncated)
            }
        } finally {
            stagedPdf.delete()
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
