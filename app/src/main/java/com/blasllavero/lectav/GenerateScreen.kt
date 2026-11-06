/*
 * LECTAV — user-initiated draft generation, review and saving
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GenerateScreen(
    course: SavedCourse,
    subjectTitle: String,
    courseTitle: String,
    suggestedTopic: String,
    chatGPTStatus: ChatGPTAuthState,
    onChatGPT: () -> Unit,
    onSaved: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var topic by rememberSaveable(course.id, suggestedTopic) { mutableStateOf(suggestedTopic) }
    var language by rememberSaveable { mutableStateOf("Català") }
    var kind by rememberSaveable { mutableStateOf("lesson") }
    var mode by rememberSaveable { mutableStateOf("demo") }
    var endpoint by rememberSaveable { mutableStateOf("http://192.168.1.10:11434") }
    var model by rememberSaveable { mutableStateOf("gemma3:4b") }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf(emptyList<ChatGPTModel>()) }
    var selectedModel by remember { mutableStateOf("") }
    var modelBusy by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf("") }
    var expandedModels by remember { mutableStateOf(false) }
    LaunchedEffect(mode, chatGPTStatus.connected, chatGPTStatus.inferenceAllowed) {
        if (mode == "chatgpt" && chatGPTStatus.inferenceAllowed) {
            modelBusy = true
            modelError = ""
            val result = runCatching { ChatGPTInference.availableModels() }
            availableModels = result.getOrDefault(emptyList())
            if (selectedModel !in availableModels.map { it.slug }) {
                selectedModel = availableModels.firstOrNull()?.slug.orEmpty()
            }
            modelError = result.exceptionOrNull()?.localizedMessage ?: ""
            modelBusy = false
        }
    }

    Text("Generació de continguts · LECTAV", style = MaterialTheme.typography.titleLarge)
    Text("$courseTitle · $subjectTitle", style = MaterialTheme.typography.titleMedium)
    Text(
        "Els esborranys es generen quan ho demanes. Revisa el contingut abans de " +
        "desar-lo, usar-lo a classe o compartir-lo.",
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value = topic, onValueChange = { topic = it.take(200); draft = ""; saved = false },
        label = { Text("Tema o concepte") }, placeholder = { Text("Ex.: equacions de primer grau") },
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = language, onValueChange = { language = it.take(40) },
        label = { Text("Idioma del material") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Text("Tipus de contingut", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = kind == "lesson", onClick = { kind = "lesson" },
            label = { Text("Teoria") })
        FilterChip(selected = kind == "exercise", onClick = { kind = "exercise" },
            label = { Text("Exercicis") })
        FilterChip(selected = kind == "solutions", onClick = { kind = "solutions" },
            label = { Text("Solucions") })
    }
    HorizontalDivider()
    Text("Motor de generació", style = MaterialTheme.typography.titleSmall)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(selected = mode == "demo", onClick = {
            mode = "demo"; draft = ""; saved = false
        },
            label = { Text("Prova de PDF sense IA") })
        FilterChip(selected = mode == "ollama", onClick = {
            mode = "ollama"; draft = ""; saved = false
        },
            label = { Text("IA oberta autoallotjada (Ollama)") })
        FilterChip(selected = mode == "chatgpt", onClick = {
            mode = "chatgpt"; draft = ""; saved = false
        }, label = { Text("ChatGPT (compte propi)") })
    }
    if (mode == "ollama") {
        Text(
            "Opcional: el text de la petició s'enviarà NOMÉS al servidor " +
            "local que indiquis. No incloguis dades personals d'alumnes. " +
            "El servidor ha d'estar configurat prèviament a la teva xarxa privada.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = endpoint, onValueChange = { endpoint = it.take(120) },
            label = { Text("Servidor LAN (IP privada)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = model, onValueChange = { model = it.take(100) },
            label = { Text("Model Ollama instal·lat") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    } else if (mode == "chatgpt") {
        Text(
            "La petició educativa s'enviarà a OpenAI amb el teu compte. " +
            "No incloguis noms, notes ni dades personals d'alumnes. " +
            "El text generat és un esborrany que cal revisar.",
            style = MaterialTheme.typography.bodySmall
        )
        if (!chatGPTStatus.connected || !chatGPTStatus.inferenceAllowed) {
            Text(
                if (chatGPTStatus.connected)
                    "Aquest compte encara no ha autoritzat l'ús del pla per generar."
                else "Cal connectar el teu compte ChatGPT.",
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(onClick = onChatGPT, modifier = Modifier.fillMaxWidth()) {
                Text("Configura la connexió amb ChatGPT")
            }
        } else {
            Text("Model disponible amb el teu compte", style = MaterialTheme.typography.labelLarge)
            if (modelBusy) CircularProgressIndicator()
            if (modelError.isNotBlank()) Text(modelError, color = MaterialTheme.colorScheme.error)
            if (availableModels.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        expandedModels = true
                    }) {
                        Text(availableModels.firstOrNull { it.slug == selectedModel }?.name
                            ?: "Selecciona un model")
                    }
                    DropdownMenu(
                        expanded = expandedModels,
                        onDismissRequest = { expandedModels = false }
                    ) {
                        availableModels.forEach { modelEntry ->
                            DropdownMenuItem(
                                text = { Text(modelEntry.name) },
                                onClick = {
                                    selectedModel = modelEntry.slug
                                    expandedModels = false
                                }
                            )
                        }
                    }
                }
            } else if (!modelBusy) {
                Text("Aquest compte no ha retornat cap model compatible.")
            }
        }
    } else {
        Text(
            "Aquesta modalitat només crea un PDF de demostració. No simula IA " +
            "ni genera apunts curriculars complets.",
            style = MaterialTheme.typography.bodySmall
        )
    }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy && topic.trim().length >= 3 &&
            (mode != "chatgpt" || (
                chatGPTStatus.inferenceAllowed && !modelBusy &&
                availableModels.any { it.slug == selectedModel }
            )),
        onClick = {
            busy = true
            error = ""
            draft = ""
            saved = false
            val request = DraftRequest(
                level = courseTitle, subject = subjectTitle, topic = topic.trim(),
                language = language, kind = when(kind) {
                    "exercise" -> "Exercicis"
                    "solutions" -> "Solucionari"
                    else -> "Teoria"
                }
            )
            scope.launch {
                val result = runCatching {
                    when (mode) {
                        "ollama" -> OnDemandGenerator.generateWithOllama(endpoint, model, request)
                        "chatgpt" -> {
                            val selected = availableModels.firstOrNull { it.slug == selectedModel }
                                ?: error("No hi ha cap model ChatGPT seleccionat.")
                            ChatGPTInference.generate(selected, request)
                        }
                        else -> OnDemandGenerator.demoDraft(request)
                    }
                }
                draft = result.getOrDefault("")
                error = result.exceptionOrNull()?.localizedMessage ?: ""
                busy = false
            }
        }
    ) { Text(if (busy) "Generant…" else "Genera l'esborrany") }
    if (busy) CircularProgressIndicator()
    if (error.isNotBlank()) {
        Text("No s'ha pogut generar: $error", color = MaterialTheme.colorScheme.error)
    }
    if (draft.isNotBlank()) {
        HorizontalDivider()
        Text("Vista prèvia editable", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(50_000); saved = false },
            minLines = 7, maxLines = 18,
            modifier = Modifier.fillMaxWidth()
        )
        Text("El PDF es desarà només quan premis el botó següent.")
        Button(
            enabled = !busy && !saved && draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true
                error = ""
                scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            OnDemandGenerator.savePdf(
                                context,
                                "$subjectTitle — ${topic.trim().take(75)}",
                                draft
                            )
                        }
                    }
                    result.onSuccess { uri ->
                        onSaved(uri.toString(), "LECTAV - ${topic.trim().take(70)}.pdf", kind, "generated")
                        saved = true
                    }.onFailure { error = it.localizedMessage ?: "Error guardant PDF" }
                    busy = false
                }
            }
        ) { Text("Desa aquest PDF a Els meus cursos") }
        if (saved) {
            Text("PDF desat. El trobaràs a Materials del curs.")
        }
    }
}

