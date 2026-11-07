/*
 * LECTAV — teacher-guided, live official curriculum import and AI analysis.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
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
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

private fun parseProposedTopics(result: String): List<TopicPlan> {
    val clean = result.trim()
        .removePrefix("```json").removePrefix("```JSON")
        .removePrefix("```").removeSuffix("```").trim()
    val root = JSONObject(clean)
    val topics = root.getJSONArray("topics")
    require(topics.length() in 4..12) {
        "La resposta ha de contenir de 4 a 12 unitats. Revisa el resultat."
    }
    return (0 until topics.length()).map { i ->
        val entry = topics.getJSONObject(i)
        val title = entry.getString("title").trim().take(150)
        val sessions = entry.getInt("sessions").coerceIn(1, 20)
        require(title.length >= 3)
        TopicPlan("proposal_${i + 1}", title, sessions, "ai-proposal")
    }
}

private fun readEditableTopics(text: String): List<TopicPlan> {
    val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    require(lines.size in 4..12) { "Introdueix de 4 a 12 unitats." }
    return lines.mapIndexed { index, line ->
        val parts = line.split("|").map(String::trim)
        require(parts.size == 2 && parts[0].length in 3..150) {
            "Cada línia ha de tenir: Títol de la unitat | sessions"
        }
        val sessions = parts[1].toIntOrNull()
        require(sessions != null && sessions in 1..20) {
            "Nombre de sessions no vàlid: ${parts[1]}"
        }
        TopicPlan("proposal_${index + 1}", parts[0], sessions, "ai-reviewed")
    }
}

@Composable
internal fun CurriculumSourceScreen(
    course: SavedCourse,
    courseTitle: String,
    subjectTitle: String,
    hasExistingPlan: Boolean,
    chatGPTStatus: ChatGPTAuthState,
    usageGuard: ChatGPTUsageGuard,
    onChatGPT: () -> Unit,
    onApplyPlan: (List<TopicPlan>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var officialUrl by rememberSaveable(course.id) {
        mutableStateOf(CurriculumDocumentLoader.initialUrl(course))
    }
    var source by remember(course.id) {
        mutableStateOf(CurriculumDocumentLoader.cached(context, course))
    }
    var loading by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var modelError by remember { mutableStateOf("") }
    var models by remember { mutableStateOf(emptyList<ChatGPTModel>()) }
    var chosenModel by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showConsent by remember { mutableStateOf(false) }
    var proposedLines by remember { mutableStateOf("") }
    var showApplyConfirm by remember { mutableStateOf(false) }
    var academicYear by rememberSaveable { mutableStateOf("2026-2027") }
    val useCount by usageGuard.state.collectAsState()

    LaunchedEffect(chatGPTStatus.inferenceAllowed) {
        if (chatGPTStatus.inferenceAllowed) {
            val result = runCatching { ChatGPTInference.availableModels() }
            models = result.getOrDefault(emptyList())
            chosenModel = models.firstOrNull()?.slug.orEmpty()
            modelError = result.exceptionOrNull()?.localizedMessage.orEmpty()
        }
    }

    Text("Currículum oficial sota demanda", style=MaterialTheme.typography.titleLarge)
    Text("$courseTitle · $subjectTitle", style=MaterialTheme.typography.titleMedium)
    Text(
        "Aquest document es descarrega quan ho demanes i s'analitza " +
        "localment. La font oficial defineix sabers, competències i " +
        "criteris, però no imposa un Tema 1 universal.",
        style=MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value=academicYear, onValueChange={ academicYear=it.take(20) },
        label={Text("Curs acadèmic (per revisar la vigència)")},
        singleLine=true, modifier=Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value=officialUrl, onValueChange={ officialUrl=it.take(700) },
        label={Text("Enllaç HTTPS directe del PDF oficial")},
        modifier=Modifier.fillMaxWidth(), minLines=2, maxLines=3
    )
    if (course.pathway=="eso") {
        OutlinedButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://xtec.gencat.cat/ca/curriculum/eso/curriculum-175-2022/")))
        }) { Text("Consulta l'índex oficial d'ESO (XTEC)") }
    }
    Text(
        "Fonts admeses en aquesta prova: portals oficials XTEC, DOGC, " +
        "BOE, gob.es i gov.uk. Per a altres territoris cal ampliar i " +
        "validar els portals. No s'importa automàticament una llei " +
        "ni se'n garanteix la vigència.",
        style=MaterialTheme.typography.bodySmall
    )
    Button(
        modifier=Modifier.fillMaxWidth(),
        enabled=!loading && !aiBusy && officialUrl.isNotBlank(),
        onClick={
            loading=true; error=""
            scope.launch {
                val result=runCatching {
                    CurriculumDocumentLoader.initialize(context)
                    CurriculumDocumentLoader.retrieve(officialUrl)
                }
                result.onSuccess { newSource ->
                    if (CurriculumDocumentLoader.save(context,course,newSource)) {
                        source=newSource
                        proposedLines=""
                    } else {
                        error="No s'ha pogut conservar la font al dispositiu."
                    }
                }.onFailure { e -> error=e.localizedMessage ?: "Error en la consulta" }
                loading=false
            }
        }
    ) { Text(if(loading) "Consultant el PDF oficial…" else "Descarrega i extreu el currículum") }
    if(loading) CircularProgressIndicator()
    if(error.isNotBlank()) {
        Text(error,color=MaterialTheme.colorScheme.error)
    }

    source?.let { document ->
        HorizontalDivider()
        Text("Font consultada",style=MaterialTheme.typography.titleMedium)
        Text(
            "Data de consulta: " +
            DateFormat.getDateTimeInstance().format(Date(document.fetchedAt)) +
            " · ${document.totalPages} pàgines",
            style=MaterialTheme.typography.bodySmall
        )
        Text("Origen: ${document.sourceUrl}",style=MaterialTheme.typography.bodySmall)
        Text(
            "SHA-256 de la font: ${document.sourceHash.take(20)}… " +
            "El text extret és una selecció, no el PDF oficial complet. " +
            "Comprova modificacions de la normativa al portal original.",
            style=MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick={expanded=!expanded}) {
            Text(if(expanded) "Amaga el text extret" else "Mostra el text extret")
        }
        if(expanded) {
            Text(document.extractedText.take(4000),style=MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
        Text("Proposta didàctica amb ChatGPT",style=MaterialTheme.typography.titleMedium)
        Text(
            "Opcional: només si prems el botó d'anàlisi, el curs, la " +
            "matèria i un extracte del PDF oficial s'enviaran a OpenAI. " +
            "És una nova petició que compta per al teu límit de LECTAV. " +
            "La resposta serà una proposta editable i NO un temari " +
            "oficialment prescrit.",
            style=MaterialTheme.typography.bodySmall
        )
        if(!chatGPTStatus.inferenceAllowed) {
            OutlinedButton(onClick=onChatGPT) {
                Text("Connecta i autoritza ChatGPT")
            }
        } else {
            Text("LECTAV no limita els intents locals; comprova la quota i " +
                "els crèdits reals a Configuració → Ús de ChatGPT.",
                style=MaterialTheme.typography.bodySmall)
            Box(modifier=Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick={menu=true}, modifier=Modifier.fillMaxWidth(),
                    enabled=models.isNotEmpty()
                ) {
                    Text(models.firstOrNull { it.slug==chosenModel }?.name
                        ?: "Selecciona un model")
                }
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    models.forEach { item ->
                        DropdownMenuItem(
                            text={Text(item.name)},
                            onClick={chosenModel=item.slug;menu=false}
                        )
                    }
                }
            }
            if(modelError.isNotBlank()) Text(modelError,color=MaterialTheme.colorScheme.error)
            Button(
                modifier=Modifier.fillMaxWidth(),
                enabled=!loading && !aiBusy &&
                    models.any { it.slug==chosenModel },
                onClick={showConsent=true}
            ) {
                Text(if(aiBusy) "Analitzant…" else "Proposa unitats amb ChatGPT")
            }
        }
    }

    if(showConsent && source != null) {
        ChatGPTUsageConfirmDialog(
            guard=usageGuard,
            onConfirm={
                showConsent=false; aiBusy=true; error=""
                val document=source!!
                val model=models.firstOrNull { it.slug==chosenModel }
                if(model==null) {
                    error="Selecciona un model compatible."; aiBusy=false
                } else scope.launch {
                    val result=runCatching {
                        val req=DraftRequest(
                            level=courseTitle + " · " + academicYear,
                            subject=subjectTitle,
                            topic="Proposta de programació didàctica amb unitats",
                            language="Català",
                            kind="Proposta de temari a partir de la font proporcionada"
                        )
                        val answer=ChatGPTInference.generate(
                            model, req, usageGuard,
                            curriculumContext="URL: ${document.sourceUrl}\n" +
                                "Consulta: ${Date(document.fetchedAt)}\n" +
                                "TEXT: ${document.extractedText}"
                        )
                        parseProposedTopics(answer)
                    }
                    result.onSuccess { topics ->
                        proposedLines=topics.joinToString("\n") {
                            "${it.title} | ${it.sessions}"
                        }
                    }.onFailure { e ->
                        error=e.localizedMessage ?: "La IA no ha retornat una proposta vàlida."
                    }
                    aiBusy=false
                }
            },
            onDismiss={showConsent=false}
        )
    }
    if(aiBusy) CircularProgressIndicator()
    if(proposedLines.isNotBlank()) {
        HorizontalDivider()
        Text("Revisa la proposta abans de desar-la",style=MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value=proposedLines, onValueChange={proposedLines=it.take(2500)},
            label={Text("Una unitat per línia: Títol | sessions")},
            modifier=Modifier.fillMaxWidth(),minLines=7,maxLines=12
        )
        if(course.role=="teacher") {
            Button(
                modifier=Modifier.fillMaxWidth(),
                onClick={showApplyConfirm=true}
            ) { Text("Desa la programació revisada al curs") }
        } else {
            Text(
                "L'alumne pot consultar la proposta, però en aquesta " +
                "versió només el professor pot substituir la programació.",
                style=MaterialTheme.typography.bodySmall
            )
        }
    }
    if(showApplyConfirm) {
        AlertDialog(
            onDismissRequest={showApplyConfirm=false},
            title={Text("Substituir la programació actual?")},
            text={Text(
                if(hasExistingPlan)
                    "Es reemplaçaran les unitats existents i es reiniciarà " +
                    "el progrés de sessions registrat per aquest curs al dispositiu. " +
                    "Confirma només si ho has revisat."
                else "Es desarà aquesta proposta de temari en aquest dispositiu. " +
                    "Podràs modificar els títols de les unitats més endavant."
            )},
            confirmButton={
                TextButton(onClick={
                    val result=runCatching { readEditableTopics(proposedLines) }
                    result.onSuccess { onApplyPlan(it); proposedLines=""; showApplyConfirm=false }
                        .onFailure { error=it.localizedMessage ?: "Temari no vàlid"; showApplyConfirm=false }
                }) { Text("Confirma i desa") }
            },
            dismissButton={
                TextButton(onClick={showApplyConfirm=false}) { Text("Cancel·la") }
            }
        )
    }
}
