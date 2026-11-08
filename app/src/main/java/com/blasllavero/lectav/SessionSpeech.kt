/*
 * LECTAV — local PDF extraction and opt-in Piper LAN synthesis.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL
import java.util.UUID

internal object SessionSpeech {
    /**
     * Extracts existing PDF text on-device; no PDF, binary data, or metadata
     * goes to a network service. A teacher reviews the editable transcript
     * before any voice synthesis request is submitted.
     */
    suspend fun extractPdf(context: Context, uri: String): String = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context.applicationContext)
        val raw = context.contentResolver.openInputStream(Uri.parse(uri))
            ?: error("No es pot obrir el PDF de teoria.")
        raw.use { source ->
            // Bound in-memory work (PDFBox may use more RAM while parsing).
            val bytes = source.readNBytes(12 * 1024 * 1024 + 1)
            require(bytes.size <= 12 * 1024 * 1024) { "El PDF supera els 12 MB." }
            PDDocument.load(bytes).use { document ->
                require(document.numberOfPages in 1..30) { "El PDF supera les 30 pàgines." }
                PDFTextStripper().getText(document)
                    .replace(Regex("""[ \t]{2,}"""), " ")
                    .trim()
                    .take(24000)
                    .also { require(it.isNotBlank()) {
                        "Aquest PDF no conté text seleccionable. Cal OCR local."
                    } }
            }
        }
    }

    /**
     * A deliberately simple two-speaker script. It is not AI-generated
     * conversation: the teacher edits and reviews each A:/B: utterance.
     */
    fun dialogueDraft(theory: String): String {
        val paragraphs = theory.split(Regex("""\n\s*\n"""))
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotEmpty() }
            .flatMap { p -> p.chunked(420) }
            .take(36)
        return buildString {
            append("A: Avui estudiarem els conceptes més importants d'aquesta sessió.\n")
            paragraphs.forEachIndexed { i, text ->
                append(if (i % 2 == 0) "B: " else "A: ")
                append(text)
                append('\n')
            }
            append("A: Revisa els exemples i practica els exercicis per consolidar l'aprenentatge.")
        }.take(24000)
    }

    private fun serviceUrl(raw: String): URL {
        val url = URL(raw.trim().trimEnd('/') + "/v1/synthesize")
        require(url.protocol == "http" && url.userInfo == null &&
            url.query == null && url.ref == null && url.path == "/v1/synthesize") {
            "Introdueix una adreça com http://192.168.1.10:8765"
        }
        require(url.host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
            "Cal una adreça IPv4 numèrica de la xarxa local."
        }
        val address = InetAddress.getByName(url.host)
        require(address is Inet4Address && address.isSiteLocalAddress) {
            "Només s'accepten servidors de la xarxa local privada."
        }
        require(url.port in 1024..65535) { "Indica un port entre 1024 i 65535." }
        return url
    }

    suspend fun synthesize(
        context: Context,
        endpoint: String,
        token: String,
        script: String,
        mode: String
    ): Uri = withContext(Dispatchers.IO) {
        require(mode in setOf("audio", "podcast"))
        require(script.length in 2..24000)
        require(token.length >= 24)
        val address = serviceUrl(endpoint)
        // Local HTTP is unencrypted. Never use untrusted Wi-Fi or include
        // personal data. No automatic request is made without user action.
        val request = JSONObject().put("mode", mode).put("text", script).toString()
        val connection = address.openConnection() as HttpURLConnection
        var output: File? = null
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 300000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) {
                throw IOException("Servidor Piper: error HTTP ${connection.responseCode}")
            }
            require(connection.contentType?.startsWith("audio/mpeg") == true) {
                "La resposta no és MP3."
            }
            require(connection.contentLengthLong <= 20 * 1024 * 1024) {
                "L'àudio supera els 20 MB."
            }
            val folder = File(context.filesDir, "generated").apply {
                check(isDirectory || mkdirs())
            }
            output = File(folder, "piper_${UUID.randomUUID()}.mp3")
            var size = 0L
            connection.inputStream.use { stream ->
                output.outputStream().use { file ->
                    val buf = ByteArray(8192)
                    while(true) {
                        val n = stream.read(buf)
                        if(n < 0) break
                        size += n
                        require(size <= 20 * 1024 * 1024) { "MP3 massa gran." }
                        file.write(buf,0,n)
                    }
                }
            }
            require(size >= 200 && output.inputStream().use { s ->
                val header = ByteArray(3)
                s.read(header) == 3 &&
                    (header.contentEquals("ID3".toByteArray()) ||
                     (header[0].toInt() and 0xff == 0xff &&
                      header[1].toInt() and 0xe0 == 0xe0))
            }) { "El servidor no ha retornat un MP3 vàlid." }
            FileProvider.getUriForFile(context, context.packageName + ".files", output)
        } catch (error: Exception) {
            output?.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }
}
