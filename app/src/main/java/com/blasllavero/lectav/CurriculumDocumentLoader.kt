/*
 * LECTAV — demand-fetched official curriculum documents.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import org.json.JSONObject

internal data class OfficialCurriculum(
    val sourceUrl: String,
    val extractedText: String,
    val fetchedAt: Long,
    val sourceHash: String,
    val verifiedHost: Boolean,
    val totalPages: Int
)

internal object CurriculumDocumentLoader {
    private const val MAX_SOURCE_BYTES = 10 * 1024 * 1024
    private const val MAX_TEXT = 24_000
    private const val KEY = "source_by_course_v1"

    const val CATALAN_MATHS_URL =
        "https://xtec.gencat.cat/web/.content/curriculum/eso/curriculum-175-2022/Matematiques.pdf"

    fun initialUrl(course: SavedCourse): String =
        if (course.pathway == "eso" && course.subject == "matematiques") {
            CATALAN_MATHS_URL
        } else ""

    private fun sourceIsOfficial(host: String): Boolean {
        val h = host.lowercase(Locale.ROOT)
        // Explicit domains/suffixes; no arbitrary server nor internal IP.
        return h == "boe.es" || h == "www.boe.es" ||
            h == "gencat.cat" || h.endsWith(".gencat.cat") ||
            h == "gob.es" || h.endsWith(".gob.es") ||
            h == "gov.uk" || h.endsWith(".gov.uk")
    }

    private fun validatedSource(text: String): URL {
        val url = URL(text.trim())
        require(url.protocol == "https") { "Només s'admeten fonts HTTPS." }
        require(url.userInfo == null && (url.port == -1 || url.port == 443) &&
            url.query == null && url.ref == null) {
            "L'URL oficial no pot incloure credencials ni paràmetres."
        }
        require(sourceIsOfficial(url.host)) {
            "Cal indicar un PDF d'un portal oficial admès (XTEC, DOGC, BOE o gov.uk)."
        }
        require(url.toString().length <= 700) { "L'URL és massa llarg." }
        return url
    }

    suspend fun retrieve(sourceUrl: String): OfficialCurriculum =
        withContext(Dispatchers.IO) {
            val url = validatedSource(sourceUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 12_000
                readTimeout = 40_000
                setRequestProperty("Accept", "application/pdf")
                setRequestProperty("User-Agent", "LECTAV/0.8.4 (curriculum consultation)")
            }
            val data = try {
                require(connection.responseCode == 200) {
                    "La font oficial ha retornat HTTP ${connection.responseCode}. " +
                    "Obre-la amb el navegador i copia l'enllaç PDF definitiu."
                }
                require(connection.contentLengthLong in -1L..MAX_SOURCE_BYTES.toLong()) {
                    "El PDF oficial supera el límit de 10 MB."
                }
                val output = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        require(output.size() + count <= MAX_SOURCE_BYTES) {
                            "El PDF supera el límit de 10 MB."
                        }
                        output.write(buffer, 0, count)
                    }
                }
                output.toByteArray()
            } finally {
                connection.disconnect()
            }
            require(data.size > 1000 && data.copyOfRange(0, 4)
                .toString(Charsets.US_ASCII) == "%PDF") {
                "L'enllaç no ha retornat un PDF. Selecciona un PDF oficial."
            }
            val context = fileLoaderContext
                ?: error("No s'ha inicialitzat el lector curricular.")
            PDFBoxResourceLoader.init(context)
            val document = PDDocument.load(data)
            val pages: Int
            val body: String
            try {
                pages = document.numberOfPages
                require(pages in 1..150) { "El PDF és massa extens per a l'anàlisi local." }
                val stripper = PDFTextStripper()
                body = stripper.getText(document)
            } finally {
                document.close()
            }
            val clean = body.replace("\u0000", "")
                .replace(Regex("""[ \t]{2,}"""), " ")
                .trim()
            require(clean.length >= 500) {
                "No s'ha pogut extreure text. El PDF podria ser escanejat; " +
                    "el reconeixement OCR encara no està disponible."
            }
            val excerpt = selectUsefulExcerpt(clean)
            val hash = MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it) }
            OfficialCurriculum(
                sourceUrl = url.toString(),
                extractedText = excerpt,
                fetchedAt = Instant.now().toEpochMilli(),
                sourceHash = hash,
                verifiedHost = true,
                totalPages = pages
            )
        }

    // The source PDF may contain 20+ pages and multiple ESO years; cap the
    // exact text sent to AI and give both broad objectives and "sabers" context.
    private fun selectUsefulExcerpt(text: String): String {
        if (text.length <= MAX_TEXT) return text
        val first = text.take(8000)
        val sabers = Regex("""\bSabers\b""", RegexOption.IGNORE_CASE)
            .findAll(text).map { it.range.first }.toList()
        val tailStart = sabers.lastOrNull { it > 8000 } ?: 8000
        val section = text.substring(tailStart.coerceAtMost(text.length - 1000))
            .take(MAX_TEXT - first.length - 80)
        return first + "\n\n[... fragment intermedi omès per límit del model ...]\n\n" + section
    }

    // Attach only the Android application context; never retain Activities.
    @Volatile private var fileLoaderContext: Context? = null
    fun initialize(context: Context) {
        fileLoaderContext = context.applicationContext
    }

    fun cached(context: Context, course: SavedCourse): OfficialCurriculum? {
        val prefs = context.getSharedPreferences("lectav_official_curriculum", Context.MODE_PRIVATE)
        val obj = runCatching {
            JSONObject(prefs.getString(courseKey(course), "") ?: "")
        }.getOrNull() ?: return null
        return runCatching {
            OfficialCurriculum(
                sourceUrl = obj.getString("url"),
                extractedText = obj.getString("text"),
                fetchedAt = obj.getLong("at"),
                sourceHash = obj.getString("hash"),
                verifiedHost = true,
                totalPages = obj.getInt("pages")
            )
        }.getOrNull()
    }

    fun save(context: Context, course: SavedCourse, source: OfficialCurriculum): Boolean {
        val prefs = context.getSharedPreferences("lectav_official_curriculum", Context.MODE_PRIVATE)
        val json = JSONObject()
            .put("url", source.sourceUrl)
            .put("text", source.extractedText)
            .put("at", source.fetchedAt)
            .put("hash", source.sourceHash)
            .put("pages", source.totalPages)
            .toString()
        return prefs.edit().putString(courseKey(course), json).commit()
    }
}
