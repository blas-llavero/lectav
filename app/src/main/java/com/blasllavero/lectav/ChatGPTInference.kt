/*
 * LECTAV — ChatGPT OAuth-authorized Responses API text-only generation.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal object ChatGPTInference {
    suspend fun availableModels(): List<ChatGPTModel> = withContext(Dispatchers.IO) {
        val token = ChatGPTAuth.bearerToken()
        val connection = URL("https://api.openai.com/v1/models")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            require(connection.responseCode == 200) {
                "No s'ha pogut consultar els models (HTTP ${connection.responseCode})."
            }
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val entries = root.optJSONArray("models") ?: JSONArray()
            buildList {
                for (i in 0 until entries.length()) {
                    val entry = entries.optJSONObject(i) ?: continue
                    if (entry.optString("visibility") != "list") continue
                    val slug = entry.optString("slug")
                    if (slug.isBlank()) continue
                    add(ChatGPTModel(slug, entry.optString("display_name", slug)))
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun generate(model: ChatGPTModel, request: DraftRequest): String =
        withContext(Dispatchers.IO) {
            val token = ChatGPTAuth.bearerToken()
            val prompt = """
                Actua com a assistent educatiu i escriu en ${request.language}.
                Nivell: ${request.level}
                Assignatura: ${request.subject}
                Tema: ${request.topic}
                Tipus de contingut: ${request.kind}

                Crea contingut educatiu original, estructurat i adequat al nivell.
                Inclou exemples, activitats i explicacions clares. Si són
                solucions, mostra passos verificables. No inventis normativa,
                competències oficials, fonts bibliogràfiques ni resultats.
                Quan no disposis de fonts curriculars verificades, indica-ho.
                Cal revisar el resultat abans de fer-lo servir o compartir-lo.
                No incloguis dades personals d'alumnes.
            """.trimIndent()
            val body = JSONObject()
                .put("model", model.slug)
                .put("input", JSONArray().put(
                    JSONObject().put("role", "user").put("content", prompt)
                ))
                .put("store", false)
                .put("stream", true)
                .toString()
            val connection = URL("https://api.openai.com/v1/responses")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 12_000
                connection.readTimeout = 120_000
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "text/event-stream")
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }
                require(connection.responseCode == 200) {
                    "No s'ha pogut començar la generació (HTTP ${connection.responseCode})."
                }
                val output = StringBuilder()
                var completed = false
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { input ->
                    while (true) {
                        val line = input.readLine() ?: break
                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]" || data.isBlank()) continue
                        val event = runCatching { JSONObject(data) }.getOrNull() ?: continue
                        when (event.optString("type")) {
                            "response.output_text.delta" -> {
                                val delta = event.optString("delta")
                                if (output.length + delta.length <= 100_000) output.append(delta)
                            }
                            "response.completed" -> {
                                completed = true
                                break
                            }
                            "response.failed" -> {
                                val err = event.optJSONObject("response")
                                    ?.optJSONObject("error")
                                val code = err?.optString("code").orEmpty()
                                throw IllegalStateException(
                                    "La generació s'ha interromput: " +
                                    if (code.isNotBlank()) code else "error del model."
                                )
                            }
                            "response.incomplete" -> error("El model no ha acabat la resposta.")
                            "error" -> error("Error del servei durant la generació.")
                        }
                    }
                }
                require(completed) { "La connexió s'ha interromput abans de completar la resposta." }
                output.toString().trim().also {
                    require(it.isNotEmpty()) { "El model ha retornat una resposta buida." }
                }
            } finally {
                connection.disconnect()
            }
        }
}
