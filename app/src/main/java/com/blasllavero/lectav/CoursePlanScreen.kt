/*
 * LECTAV — indexed three-term, teacher-editable lessons and learner progress.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CoursePlanScreen(
    course: SavedCourse,
    plan: CoursePlan?,
    files: List<LocalMaterial>,
    onRename: (String, String) -> Unit,
    onRenameSession: (String, Int, String) -> Unit,
    onDone: (String, Int, Boolean) -> Unit,
    onGenerateMaterial: (String, String, String) -> Unit,
    onOpenMaterial: (String) -> Unit,
    onLinkMaterial: (String, String) -> Unit,
    onImportVideo: (String, String, String) -> Unit,
    onImportAudio: (String, String, String, String) -> Unit,
    onGenerateSpeech: (String, String, String) -> Unit,
    onCurriculum: () -> Unit,
    onAiProgramme: () -> Unit
) {
    val context = LocalContext.current
    var pendingVideoKey by remember { mutableStateOf("") }
    var pendingVideoName by remember { mutableStateOf("") }
    var pendingAudioKey by remember { mutableStateOf("") }
    var pendingAudioName by remember { mutableStateOf("") }
    var pendingAudioType by remember { mutableStateOf("podcast") }
    var importError by remember { mutableStateOf("") }
    val videoPicker=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if(uri!=null && pendingVideoKey.isNotBlank()) {
            val result=runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val kind=context.contentResolver.getType(uri).orEmpty()
                require(kind=="video/mp4" ||
                    uri.toString().lowercase().endsWith(".mp4")) {
                    "Selecciona un vídeo en format MP4."
                }
                onImportVideo(uri.toString(), pendingVideoName, pendingVideoKey)
            }
            importError=result.exceptionOrNull()?.localizedMessage.orEmpty()
        }
        pendingVideoKey=""
        pendingVideoName=""
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && pendingAudioKey.isNotBlank()) {
            val result = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val mime = context.contentResolver.getType(uri).orEmpty()
                require(mime == "audio/mpeg" || mime == "audio/mp3") {
                    "Selecciona un fitxer d’àudio MP3."
                }
                onImportAudio(uri.toString(), pendingAudioName, pendingAudioKey, pendingAudioType)
            }
            importError = result.exceptionOrNull()?.localizedMessage.orEmpty()
        }
        pendingAudioKey = ""
        pendingAudioName = ""
    }
    Text("Programació didàctica · Índex de sessions",
        style = MaterialTheme.typography.titleLarge)
    Text(
        "Pas 1: genera i revisa la programació trimestral. " +
        "Pas 2: per a cada sessió, crea teoria, exercicis i solucions " +
        "amb el mateix títol; els documents queden vinculats " +
        "a la sessió. També hi pots associar un vídeo MP4. " +
        "La generació automàtica de l'avatar encara està pendent.",
        style = MaterialTheme.typography.bodySmall
    )
    if(importError.isNotBlank()) Text(
        importError, color=MaterialTheme.colorScheme.error
    )
    if (course.role == "teacher") {
        Button(onClick = onAiProgramme, modifier = Modifier.fillMaxWidth()) {
            Text("Genera una programació trimestral amb ChatGPT")
        }
    }
    OutlinedButton(onClick = onCurriculum, modifier = Modifier.fillMaxWidth()) {
        Text("Consulta el currículum oficial de la matèria")
    }
    if (plan == null || plan.topics.isEmpty()) {
        Text(
            if (course.role == "teacher")
                "Encara no hi ha cap programació. Selecciona un trimestre i " +
                "demana a ChatGPT que proposi les unitats i les sessions, " +
                "sense continguts precarregats."
            else "Aquest curs encara no té cap programació desada en " +
                "aquest dispositiu.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    val total = plan.topics.sumOf { it.sessions }
    val done = plan.topics.sumOf { it.completedSessions.size }
    Text("Progrés: $done de $total sessions", style = MaterialTheme.typography.titleMedium)
    LinearProgressIndicator(
        progress = { if (total == 0) 0f else done.toFloat() / total },
        modifier = Modifier.fillMaxWidth()
    )

    for (term in 1..3) {
        val units = plan.topics.filter { it.trimester == term }
        val title = when (term) {
            1 -> "Primer trimestre"
            2 -> "Segon trimestre"
            else -> "Tercer trimestre"
        }
        HorizontalDivider()
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (units.isEmpty()) {
            Text("Encara no s'han generat unitats per a aquest trimestre.",
                style = MaterialTheme.typography.bodySmall)
        }
        units.forEachIndexed { unitIndex, unit ->
            val trimesterSessionOffset = units.take(unitIndex).sumOf { it.sessions }
            var titleEdit by remember(unit.id,unit.title) { mutableStateOf(unit.title) }
            var expanded by remember(unit.id) { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Unitat ${unitIndex + 1} · ${unit.title}",
                        style = MaterialTheme.typography.titleSmall)
                    if (course.role == "teacher") {
                        OutlinedTextField(
                            value = titleEdit,
                            onValueChange = { titleEdit = it.take(150) },
                            label = { Text("Nom de la unitat") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (titleEdit.trim() != unit.title) {
                            OutlinedButton(
                                enabled = titleEdit.trim().length >= 3,
                                onClick = { onRename(unit.id, titleEdit) }
                            ) { Text("Actualitza el nom de la unitat") }
                        }
                    }
                    Text("${unit.completedSessions.size} de ${unit.sessions} sessions completades")
                    OutlinedButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Amaga l'índex de sessions"
                            else "Mostra l'índex de sessions")
                    }
                    if (expanded) {
                        (1..unit.sessions).forEach { number ->
                            val lesson = unit.sessionTitle(number)
                            var editing by remember(unit.id,number) { mutableStateOf(false) }
                            var edited by remember(unit.id,number,lesson) {
                                mutableStateOf(lesson)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Sessió ${trimesterSessionOffset + number} · $lesson",
                                    style = MaterialTheme.typography.bodyMedium)
                                val sessionNumber=trimesterSessionOffset + number
                                val sessionId="$term|${unit.id}|$number"
                                val sessionTopic="Sessió $sessionNumber - $lesson"
                                val unitNumber = plan.topics.indexOfFirst { it.id == unit.id } + 1
                                val fileStem = MaterialFileNames.sessionStem(unitNumber,sessionNumber,lesson)
                                val attached=files.filter {
                                    it.sessionKey == sessionId
                                }
                                Text(
                                    "Materials: " + listOf(
                                        "Teoria ${attached.count { it.type=="lesson" }}",
                                        "Exercicis ${attached.count { it.type=="exercise" }}",
                                        "Solucions ${attached.count { it.type=="solutions" }}",
                                        "Avatar ${attached.count { it.type=="video" }}",
                                        "Àudio ${attached.count { it.type=="audio" }}",
                                        "Podcast ${attached.count { it.type=="podcast" }}",
                                        "Transcripció ${attached.count { it.type=="transcript" }}"
                                    ).joinToString(" · "),
                                    style=MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (course.role == "student") {
                                        Checkbox(
                                            checked = number in unit.completedSessions,
                                            onCheckedChange = { onDone(unit.id, number, it) }
                                        )
                                    }
                                    if (course.role == "teacher") {
                                        TextButton(onClick = { editing = !editing }) {
                                            Text(if (editing) "Tanca" else "Edita el tema")
                                        }
                                    }
                                }
                                // Each saved material is accessible directly
                                // from the originating session, independently
                                // of its filename or later lesson title edits.
                                listOf(
                                    "lesson" to "Teoria",
                                    "exercise" to "Exercicis",
                                    "solutions" to "Solucions",
                                    "video" to "Avatar",
                                    "audio" to "Àudio",
                                    "podcast" to "Podcast",
                                    "transcript" to "Transcripció"
                                ).forEach { (type, label) ->
                                    val matching=attached.filter { it.type==type }
                                    val latest=matching.lastOrNull()
                                    Row(
                                        modifier=Modifier.fillMaxWidth(),
                                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(label, modifier=Modifier.weight(1f),
                                            style=MaterialTheme.typography.bodyMedium)
                                        if(latest!=null) {
                                            Button(onClick={onOpenMaterial(latest.id)}) {
                                                Text("Obre")
                                            }
                                        }
                                        if(type=="lesson" || type=="exercise" || type=="solutions" || type=="transcript") {
                                            OutlinedButton(onClick={
                                                onGenerateMaterial(sessionId,sessionTopic,type)
                                            }) {
                                                Text(if(latest==null) "Genera" else "Nou")
                                            }
                                        } else if(latest==null) {
                                            Text("Pendent",style=MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    // Older versions remain accessible; no
                                    // automatically overwritten PDF/MP4 files.
                                    matching.forEach { file ->
                                        TextButton(onClick={onOpenMaterial(file.id)}) {
                                            Text(file.name)
                                        }
                                    }
                                }
                                if(course.role=="teacher") {
                                    val available=files.filter { it.sessionKey.isBlank() }
                                    if(available.isNotEmpty()) {
                                        var showAssign by remember(sessionId) {
                                            mutableStateOf(false)
                                        }
                                        Box(modifier=Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick={showAssign=true},
                                                modifier=Modifier.fillMaxWidth()
                                            ) { Text("Vincula un document anterior a aquesta sessió") }
                                            DropdownMenu(
                                                expanded=showAssign,
                                                onDismissRequest={showAssign=false}
                                            ) {
                                                available.take(40).forEach { previous ->
                                                    DropdownMenuItem(
                                                        text={Text(previous.name)},
                                                        onClick={
                                                            showAssign=false
                                                            onLinkMaterial(previous.id,sessionId)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    modifier=Modifier.fillMaxWidth(),
                                    onClick={
                                        pendingVideoKey=sessionId
                                        pendingVideoName=MaterialFileNames.forSessionStem(fileStem,"video")
                                        videoPicker.launch(arrayOf("video/mp4"))
                                    }
                                ) { Text("Importa un vídeo de l'avatar (MP4)") }
                                if(course.role == "teacher") {
                                    OutlinedButton(
                                        modifier=Modifier.fillMaxWidth(),
                                        enabled=attached.any { it.type=="lesson" },
                                        onClick={onGenerateSpeech(sessionId,fileStem,sessionTopic)}
                                    ) { Text("Genera àudio o podcast des de la teoria (Piper)") }
                                    listOf("audio" to "àudio", "podcast" to "podcast").forEach { (type, label) ->
                                        OutlinedButton(
                                            modifier=Modifier.fillMaxWidth(),
                                            onClick={
                                                pendingAudioKey=sessionId
                                                pendingAudioType=type
                                                pendingAudioName=MaterialFileNames.forSessionStem(fileStem,type)
                                                audioPicker.launch(arrayOf("audio/mpeg"))
                                            }
                                        ) { Text("Importa un $label (MP3)") }
                                    }
                                }
                                OutlinedButton(
                                    enabled=false, onClick={},
                                    modifier=Modifier.fillMaxWidth()
                                ) { Text("Genera el vídeo de l'avatar (fase futura)") }
                                if (course.role == "teacher" && editing) {
                                    OutlinedTextField(
                                        value = edited,
                                        onValueChange = { edited = it.take(150) },
                                        label = { Text("Tema que s'impartirà") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3
                                    )
                                    OutlinedButton(
                                        enabled = edited.trim().length >= 3,
                                        onClick = {
                                            onRenameSession(unit.id,number,edited)
                                            editing = false
                                        }
                                    ) { Text("Desa el tema de la sessió") }
                                }
                            }
                            if (number != unit.sessions) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
