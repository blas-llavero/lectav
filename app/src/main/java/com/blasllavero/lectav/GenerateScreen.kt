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
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

@Composable
internal fun GenerateScreen(
    course: SavedCourse,
    subjectTitle: String,
    courseTitle: String,
    suggestedTopic: String,
    preferredKind: String = "lesson",
    sessionKey: String = "",
    sessionFileStem: String = "",
    chatGPTStatus: ChatGPTAuthState,
    usageGuard: ChatGPTUsageGuard,
    onChatGPT: () -> Unit,
    onSaved: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generatorPrefs = remember(context.applicationContext) {
        context.applicationContext.getSharedPreferences("lectav_generator", android.content.Context.MODE_PRIVATE)
    }
    val usage by usageGuard.state.collectAsState()
    var showUsageConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { usageGuard.refresh() }
    var topic by rememberSaveable(course.id, suggestedTopic) { mutableStateOf(suggestedTopic) }
    var language by rememberSaveable { mutableStateOf(generatorPrefs.getString("language","Català") ?: "Català") }
    var kind by rememberSaveable(course.id, suggestedTopic, preferredKind) {
        mutableStateOf(if (sessionKey.isNotBlank()) preferredKind
            else generatorPrefs.getString("kind","lesson") ?: "lesson")
    }
    var mode by rememberSaveable { mutableStateOf(generatorPrefs.getString("mode","demo") ?: "demo") }
    var endpoint by rememberSaveable { mutableStateOf(generatorPrefs.getString("endpoint","http://192.168.1.10:11434") ?: "http://192.168.1.10:11434") }
    var model by rememberSaveable { mutableStateOf(generatorPrefs.getString("ollamaModel","gemma3:4b") ?: "gemma3:4b") }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var editorExpanded by remember { mutableStateOf(false) }
    var savingPdf by remember { mutableStateOf(false) }
    var savingJob by remember { mutableStateOf<Job?>(null) }
    var savedFileUri by remember { mutableStateOf("") }
    var availableModels by remember { mutableStateOf(emptyList<ChatGPTModel>()) }
    var selectedModel by remember { mutableStateOf(generatorPrefs.getString("chatgptModel","") ?: "") }
    LaunchedEffect(language, kind, mode, endpoint, model, selectedModel) {
        generatorPrefs.edit()
            .putString("language",language.take(40))
            .putString("kind",kind.take(20))
            .putString("mode",mode.take(20))
            .putString("endpoint",endpoint.take(120))
            .putString("ollamaModel",model.take(100))
            .putString("chatgptModel",selectedModel.take(100))
            .apply()
    }
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


    fun generatedFilename(): String = if(sessionFileStem.isNotBlank())
        MaterialFileNames.forSessionStem(sessionFileStem,kind)
    else MaterialFileNames.forDraft(topic,kind)

    fun generateDraft() {
            busy = true
            error = ""
            draft = ""
            saved = false
            editorExpanded = false
            savedFileUri = ""
            val request = DraftRequest(
                level = courseTitle, subject = subjectTitle, topic = topic.trim(),
                language = language, kind = when(kind) {
                    "exercise" -> "Exercicis"
                    "solutions" -> "Solucionari"
                    "transcript" -> "Transcripció de la teoria, en format de guió escrit per a lectura o locució. No afirmis que procedeix de cap gravació d’àudio."
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
                            ChatGPTInference.generate(selected, request, usageGuard)
                        }
                        else -> OnDemandGenerator.demoDraft(request)
                    }
                }
                draft = result.getOrDefault("")
                error = result.exceptionOrNull()?.localizedMessage ?: ""
                busy = false
            }
    }

    Text("Generació de continguts · LECTAV", style = MaterialTheme.typography.titleLarge)
    if(sessionKey.isNotBlank()) {
        Text(
            "Material vinculat a la sessió de la programació. " +
                "Tindrà el nom de la sessió i quedarà associat al seu " +
                "temari; també es conservarà a la biblioteca de materials.",
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text("$courseTitle · $subjectTitle", style = MaterialTheme.typography.titleMedium)
    Text(
        "Els esborranys es generen quan ho demanes. Revisa el contingut abans de " +
        "desar-lo, usar-lo a classe o compartir-lo.",
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value = topic, onValueChange = {
            topic = it.take(200); draft = ""; saved = false; savedFileUri = ""
        },
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
    if (sessionKey.isNotBlank()) {
        FilterChip(selected = kind == "transcript", onClick = { kind = "transcript" },
            label = { Text("Guió / transcripció PDF") })
        Text("Aquest guió es genera a partir de la petició de la sessió; no és una transcripció automàtica d’un MP3.",
            style = MaterialTheme.typography.bodySmall)
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
            "LECTAV no imposa un nombre màxim d'intents. ChatGPT sí " +
            "que pot aplicar límits del compte o consumir crèdits " +
            "autoritzats; consulta Configuració → Ús.",
            style = MaterialTheme.typography.bodySmall
        )
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
            if (mode == "chatgpt") showUsageConfirm = true else generateDraft()
        }
    ) { Text(if (busy) "Generant…" else "Genera l'esborrany") }
    if (showUsageConfirm) {
        ChatGPTUsageConfirmDialog(
            guard = usageGuard,
            onConfirm = {
                showUsageConfirm = false
                generateDraft()
            },
            onDismiss = { showUsageConfirm = false }
        )
    }
    if (busy) CircularProgressIndicator()
    if (error.isNotBlank()) {
        Text("No s'ha pogut generar: $error", color = MaterialTheme.colorScheme.error)
    }
    if (draft.isNotBlank()) {
        HorizontalDivider()
        Text("Esborrany del PDF", style = MaterialTheme.typography.titleMedium)
        // A 20–50k character Compose OutlinedTextField in a vertically
        // scrolling screen can repeatedly relayout the entire document and
        // freeze lower-powered tablets even AFTER the PDF was exported.
        // Keep large editors collapsed until the user explicitly requests one.
        val editorAllowed = editorExpanded || (!saved && draft.length <= 6000)
        if (editorAllowed) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(50_000); saved = false },
                minLines = 7, maxLines = 18,
                modifier = Modifier.fillMaxWidth()
            )
            if (draft.length > 6000) {
                TextButton(onClick = { editorExpanded = false }) {
                    Text("Plega l'editor per alleugerir la tauleta")
                }
            }
        } else {
            Text(
                if (saved)
                    "El PDF està desat. Hem plegat l'editor per evitar bloquejos."
                else "Esborrany extens (${draft.length} caràcters). " +
                    "La vista curta evita bloquejos durant la maquetació.",
                style = MaterialTheme.typography.bodySmall
            )
            if (!saved) {
                Text(draft.take(1300) + if (draft.length > 1300) "…" else "")
            }
            OutlinedButton(onClick = { editorExpanded = true }) {
                Text("Mostra i edita el text complet")
            }
        }
        Text(
            "Les fórmules LaTeX, la negreta, els títols i les taules es " +
            "maquetaran al PDF. Si alguna fórmula no es pot interpretar, " +
            "LECTAV t'indicarà que la revisis abans de desar el document."
        )
        Text(
            "LECTAV desa primer el PDF a la seva carpeta privada de la memòria " +
            "interna. Després podràs exportar-ne una còpia al directori que " +
            "triïs, inclosa la targeta SD, sense generar de nou el contingut."
        )
        Text("El PDF es desarà només quan premis el botó següent.")
        Button(
            enabled = !busy && !saved && draft.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true
                savingPdf = true
                error = ""
                savingJob = scope.launch {
                    val result = runCatching {
                        TypesetPdfExporter.save(
                            context,
                            generatedFilename().removeSuffix(".pdf"),
                            draft
                        )
                    }
                    result.onSuccess { uri ->
                        onSaved(
                            uri.toString(), generatedFilename(),
                            kind, "generated", sessionKey
                        )
                        savedFileUri = uri.toString()
                        saved = true
                        editorExpanded = false
                    }.onFailure {
                        error = if (it is kotlinx.coroutines.CancellationException)
                            "S'ha cancel·lat la creació del PDF."
                        else it.localizedMessage ?: "Error guardant PDF"
                    }
                    savingPdf = false
                    busy = false
                    savingJob = null
                }
            }
        ) { Text("Desa aquest PDF a Els meus cursos") }
        if (savingPdf) {
            Text(
                "LECTAV està maquetant i desant el PDF. Pots cancel·lar " +
                "aquesta operació si triga massa; no cal tancar l'app.",
                style = MaterialTheme.typography.bodySmall
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { savingJob?.cancel() }) {
                Text("Cancel·la la creació del PDF")
            }
        }
        if (saved) {
            Text(
                "PDF desat a la memòria privada de LECTAV. " +
                "Per trobar-lo amb CX File Explorer, desa'n una còpia " +
                "a Documents, Descàrregues o una carpeta de la targeta SD."
            )
            if (savedFileUri.isNotBlank()) {
                ExportDocumentButton(
                    sourceUri = savedFileUri,
                    displayName = generatedFilename(),
                    mime = "application/pdf"
                )
            }
        }
    }
}

