/*
 * LECTAV — fully local, teacher-reviewed Piper Android speech.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun SessionSpeechScreen(
    sessionTitle: String,
    filenameStem: String,
    sessionKey: String,
    materials: List<LocalMaterial>,
    onSaved: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theory = materials.lastOrNull { it.type == "lesson" }
    var mode by remember { mutableStateOf("audio") }
    var script by remember(sessionKey) { mutableStateOf("") }
    var loadedTheory by remember(sessionKey) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var voiceList by remember { mutableStateOf<List<OnDeviceSpeech.OfflineVoice>>(emptyList()) }
    var voiceA by remember { mutableStateOf("") }
    var voiceB by remember { mutableStateOf("") }
    var aExpanded by remember { mutableStateOf(false) }
    var bExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(sessionKey) {
        runCatching { OnDeviceSpeech.voices(context) }
            .onSuccess { choices ->
                voiceList=choices
                voiceA=choices.firstOrNull()?.name.orEmpty()
                voiceB=choices.getOrNull(1)?.name.orEmpty()
                if(choices.isEmpty()) message="Piper Android no té cap veu instal·lada."
            }
            .onFailure { message=it.localizedMessage ?: "No es pot obrir Piper Android." }
    }

    Text("Genera àudio i podcast sense PC",style=MaterialTheme.typography.titleLarge)
    Text(sessionTitle,style=MaterialTheme.typography.titleMedium)
    Text(
        "La teoria s'extreu al dispositiu. Piper Android produeix l'àudio " +
            "sense enviar el guió a Internet ni utilitzar cap PC. " +
            "Instal·la primer Piper Android i les veus lliures compatibles.",
        style=MaterialTheme.typography.bodySmall
    )
    if(theory==null) {
        Text("Primer cal generar o vincular la teoria PDF de la sessió.")
    } else {
        OutlinedButton(
            enabled=!busy,modifier=Modifier.fillMaxWidth(),
            onClick={
                busy=true;message=""
                scope.launch {
                    runCatching { SessionSpeech.extractPdf(context,theory.uri) }
                        .onSuccess { script=it.take(8000);loadedTheory=true
                            message="Teoria carregada. Revisa el guió i les fórmules." }
                        .onFailure { message=it.localizedMessage ?: "No es pot llegir la teoria PDF." }
                    busy=false
                }
            }
        ) { Text("Carrega la teoria del PDF") }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(selected=mode=="audio",onClick={mode="audio"},
            label={Text("Àudio d'una veu")})
        FilterChip(selected=mode=="podcast",onClick={
            if(mode!="podcast" && script.isNotBlank() && !script.lines().any {
                it.startsWith("A:") || it.startsWith("B:")
            }) script=SessionSpeech.dialogueDraft(script).take(8000)
            mode="podcast"
        },label={Text("Podcast de dues veus")})
    }
    if(mode=="podcast") {
        Text("Fes servir A: i B: per separar les intervencions. " +
            "Aquest primer guió només alterna les veus: revisa preguntes, " +
            "respostes i notació matemàtica abans de generar l'àudio.",
            style=MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled=!busy && script.isNotBlank(),
            onClick={script=SessionSpeech.dialogueDraft(script).take(8000)}) {
            Text("Prepara un esborrany de dues veus")
        }
    }
    OutlinedTextField(value=script,onValueChange={script=it.take(8000)},
        label={Text("Guió revisable")},minLines=6,maxLines=12,
        modifier=Modifier.fillMaxWidth())
    Text("${script.length} / 8.000 caràcters",
        style=MaterialTheme.typography.bodySmall)

    if(voiceList.isEmpty()) {
        Text("No hi ha veus Piper disponibles. Instal·la Piper Android, " +
            "descarrega una veu a la seva aplicació i torna a entrar a la sessió.",
            style=MaterialTheme.typography.bodySmall)
    } else {
        Text("Veu principal (A)",style=MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick={aExpanded=true},modifier=Modifier.fillMaxWidth()) {
                Text(voiceList.firstOrNull { it.name==voiceA }?.label ?: "Selecciona una veu")
            }
            DropdownMenu(expanded=aExpanded,onDismissRequest={aExpanded=false}) {
                voiceList.forEach { voice ->
                    DropdownMenuItem(text={Text(voice.label)},onClick={
                        voiceA=voice.name;aExpanded=false
                    })
                }
            }
        }
        if(mode=="podcast") {
            Text("Segona veu (B)",style=MaterialTheme.typography.titleSmall)
            Box {
                OutlinedButton(onClick={bExpanded=true},modifier=Modifier.fillMaxWidth()) {
                    Text(voiceList.firstOrNull { it.name==voiceB }?.label ?: "Selecciona la segona veu")
                }
                DropdownMenu(expanded=bExpanded,onDismissRequest={bExpanded=false}) {
                    voiceList.forEach { voice ->
                        DropdownMenuItem(text={Text(voice.label)},onClick={
                            voiceB=voice.name;bExpanded=false
                        })
                    }
                }
            }
        }
    }
    Text(
        "L'àudio local s'exporta com a WAV PCM. És més gran que un MP3 però " +
            "no necessita cap codificador extern. Per al podcast, " +
            "les dues veus han de tenir la mateixa freqüència de mostreig.",
        style=MaterialTheme.typography.bodySmall
    )
    Button(
        enabled=!busy && loadedTheory && script.trim().length >= 10 &&
            voiceA.isNotBlank() && (mode!="podcast" ||
                (voiceB.isNotBlank() && voiceA!=voiceB)),
        modifier=Modifier.fillMaxWidth(),
        onClick={
            busy=true;message=""
            scope.launch {
                runCatching {
                    OnDeviceSpeech.synthesize(context,script.trim(),mode,voiceA,voiceB)
                }.onSuccess { uri ->
                    val filename=MaterialFileNames.forSessionStem(filenameStem,mode)
                        .removeSuffix(".mp3")+".wav"
                    onSaved(uri,filename,mode)
                    message="Àudio WAV generat i vinculat a la sessió."
                }.onFailure { message=it.localizedMessage ?: "No s'ha pogut generar l'àudio." }
                busy=false
            }
        }
    ) { Text(if(busy) "Generant WAV localment…" else "Genera i desa àudio WAV") }
    if(busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
    OutlinedButton(
        enabled=!busy && loadedTheory && script.trim().isNotEmpty(),
        modifier=Modifier.fillMaxWidth(),
        onClick={
            busy=true;message=""
            scope.launch {
                runCatching {
                    TypesetPdfExporter.save(context,
                        MaterialFileNames.forSessionStem(filenameStem,"transcript").removeSuffix(".pdf"),
                        script.trim())
                }.onSuccess { uri ->
                    onSaved(uri.toString(),
                        MaterialFileNames.forSessionStem(filenameStem,"transcript"),"transcript")
                    message="Guió revisat desat com a transcripció PDF."
                }.onFailure { message=it.localizedMessage ?: "No s'ha pogut desar la transcripció." }
                busy=false
            }
        }
    ) { Text("Desa el guió com a transcripció PDF") }
    if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
}
