/*
 * LECTAV — on-demand drafts using optional privately hosted OSS AI.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.net.Uri
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DraftRequest(
    val level: String,
    val subject: String,
    val topic: String,
    val language: String,
    val kind: String
)

internal object OnDemandGenerator {
    // Demonstration content, not AI. No textbook material and no fabricated
    // official curriculum validation; deliberately short so it is reviewable.
    fun demoDraft(request: DraftRequest): String = """
        DEMOSTRACIÓ LOCAL — NO GENERAT AMB IA
        Curs: ${request.level}
        Matèria: ${request.subject}
        Tema demanat: ${request.topic}
        Tipus: ${request.kind}
        Idioma: ${request.language}

        Aquest PDF demostra la generació local de documents, la vista prèvia i
        l'emmagatzematge privat. NO és una unitat didàctica completa.

        Comprova sempre les fonts curriculars oficials vigents i adapta
        explicacions, activitats i solucions abans de compartir-les.
    """.trimIndent()

    // Only literal numeric LAN hosts may be selected: no arbitrary domains,
    // redirects, user credentials or internet addresses. Explicit opt-in UI.
    private fun localEndpoint(text: String): URL {
        val uri = Uri.parse(text.trim())
        require(uri.scheme == "http" && uri.userInfo == null && uri.query == null &&
            uri.fragment == null && uri.path.orEmpty().trim('/') .isEmpty()) {
            "Use http://<local-LAN-IP>:11434"
        }
        val host = uri.host.orEmpty()
        require(host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
            "Use a numeric private IPv4 address"
        }
        val addr = InetAddress.getByName(host)
        require(addr is Inet4Address && (addr.isSiteLocalAddress || addr.isLoopbackAddress) &&
            !addr.isAnyLocalAddress) { "Only private LAN endpoints are allowed" }
        val port = if (uri.port == -1) 11434 else uri.port
        require(port in 1..65535) { "Invalid port" }
        return URL("http://$host:$port/api/generate")
    }

    suspend fun generateWithOllama(
        endpoint: String,
        model: String,
        request: DraftRequest
    ): String = withContext(Dispatchers.IO) {
        require(model.matches(Regex("""[A-Za-z0-9_.:/-]{1,100}"""))) {
            "Provide a valid installed model identifier"
        }
        require(request.topic.trim().length in 3..200) { "Provide a topic (3–200 chars)" }
        val prompt = """
            You are an educational content draft assistant. Write in ${request.language}.
            Education level: ${request.level}. Subject: ${request.subject}.
            Topic: ${request.topic}. Content kind: ${request.kind}.
            Create an ORIGINAL age-appropriate teaching draft with clear explanations,
            structured activities, and examples. For exercise sheets, include tasks;
            for answer keys, show transparent worked steps.
            Never claim a syllabus is official unless verified. Do not invent sources,
            curriculum citations, exam grades or accreditation.
            Do not request personal data. Output concise plain text, no Markdown tables.
            End with a reminder to review the draft before teaching or sharing.
        """.trimIndent()
        val payload = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("stream", false)
            .put("options", JSONObject().put("num_predict", 1400))
            .toString()
        val connection = localEndpoint(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 120000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            require(connection.responseCode == 200) {
                "Local model returned HTTP ${connection.responseCode}"
            }
            val json = connection.inputStream.bufferedReader().use { reader ->
                // Bound the returned text to avoid pathological memory usage.
                reader.readText().take(160_000)
            }
            JSONObject(json).optString("response").trim().also {
                require(it.isNotBlank()) { "The model returned no draft" }
            }
        } finally {
            connection.disconnect()
        }
    }

}
