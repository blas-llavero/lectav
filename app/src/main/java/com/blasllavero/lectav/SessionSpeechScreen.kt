/*
 * LECTAV — teacher-approved spoken lesson generation.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val prefs = remember(context) {
        context.getSharedPreferences("lectav_piper_settings", android.content.Context.MODE_PRIVATE)
    }
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint","http://192.168.1.10:8765").orEmpty()) }
    // Access token is intentionally not persisted.
    var token by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("audio") }
    var text by remember(sessionKey) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var hasTheory by remember(sessionKey) { mutableStateOf(false) }

    Text("Genera àudio / podcast amb Piper", style=MaterialTheme.typography.titleLarge)
    Text(sessionTitle, style=MaterialTheme.typography.titleMedium)
    Text(
        "El PDF de teoria es llegeix localment. Revisa i modifica el text abans d'enviar " +
            "només el guió al teu ordinador amb Piper, dins la xarxa privada. " +
            "No utilitzis xarxes Wi-Fi públiques ni dades personals: HTTP local no està xifrat.",
        style=MaterialTheme.typography.bodySmall
    )
    if(theory == null) {
        Text("Primer cal generar o vincular un PDF de teoria a aquesta sessió.")
    } else {
        OutlinedButton(
            enabled=!busy,
            modifier=Modifier.fillMaxWidth(),
            onClick={
                busy=true;message=""
                scope.launch {
                    runCatching { SessionSpeech.extractPdf(context,theory.uri) }
                        .onSuccess { text=it;hasTheory=true;message="Text extret. Revisa'l abans de generar la veu." }
                        .onFailure { message=it.localizedMessage ?: "No s'ha pogut llegir la teoria." }
                    busy=false
                }
            }
        ) { Text("Carrega el text del PDF de teoria") }
    }

    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected=mode=="audio",
            onClick={mode="audio"},
            label={Text("Una veu")}
        )
        FilterChip(
            selected=mode=="podcast",
            onClick={
                if(mode!="podcast" && text.isNotBlank() && !text.lines().any {
                    it.startsWith("A:") || it.startsWith("B:")
                }) text=SessionSpeech.dialogueDraft(text)
                mode="podcast"
            },
            label={Text("Podcast (dues veus)")}
        )
    }
    if(mode=="podcast") {
        Text(
            "Per al podcast, cada intervenció ha de començar amb «A:» o «B:». " +
            "LECTAV reparteix el text inicial entre dues veus, però no escriu " +
            "una conversa pedagògica amb IA: edita les preguntes i respostes.",
            style=MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick={text=SessionSpeech.dialogueDraft(text)},enabled=text.isNotBlank()&&!busy) {
            Text("Prepara esborrany de dues veus")
        }
    }
    OutlinedTextField(
        value=text,
        onValueChange={text=it.take(24000)},
        label={Text("Guió editable de la sessió")},
        modifier=Modifier.fillMaxWidth(),
        minLines=6,
        maxLines=12
    )
    Text("${text.length} / 24.000 caràcters",style=MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value=endpoint,
        onValueChange={endpoint=it.take(100)},
        label={Text("Servidor Piper de la xarxa local")},
        singleLine=true,
        modifier=Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value=token,
        onValueChange={token=it.take(200)},
        label={Text("Clau d'accés al servidor (no es desa)")},
        visualTransformation=PasswordVisualTransformation(),
        singleLine=true,
        modifier=Modifier.fillMaxWidth()
    )
    Text("Cal executar el servidor Piper lliure al teu PC i tenir dues veus instal·lades per al podcast.",
        style=MaterialTheme.typography.bodySmall)
    Button(
        enabled=!busy && text.trim().length >= 10 && token.length >= 24 && theory!=null && hasTheory,
        modifier=Modifier.fillMaxWidth(),
        onClick={
            busy=true;message=""
            prefs.edit().putString("endpoint",endpoint).apply()
            scope.launch {
                val result=runCatching { SessionSpeech.synthesize(context,endpoint,token,text.trim(),mode) }
                result.onSuccess {
                    val filename=MaterialFileNames.forSessionStem(filenameStem,mode)
                    onSaved(it.toString(),filename,mode)
                    message="Àudio generat. El fitxer apareixerà a la sessió i a Materials."
                }.onFailure { message=it.localizedMessage ?: "Ha fallat el motor Piper." }
                busy=false
            }
        }
    ) { Text(if(busy) "Generant amb Piper…" else "Genera i desa MP3") }
    if(busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
    OutlinedButton(
        enabled=!busy && text.trim().isNotBlank() && theory!=null && hasTheory,
        modifier=Modifier.fillMaxWidth(),
        onClick={
            busy=true;message=""
            scope.launch {
                runCatching {
                    TypesetPdfExporter.save(
                        context,
                        MaterialFileNames.forSessionStem(filenameStem,"transcript").removeSuffix(".pdf"),
                        text.trim()
                    )
                }.onSuccess { uri ->
                    onSaved(
                        uri.toString(),
                        MaterialFileNames.forSessionStem(filenameStem,"transcript"),
                        "transcript"
                    )
                    message="Guió revisat desat com a transcripció PDF de la sessió."
                }.onFailure { message=it.localizedMessage ?: "No s'ha pogut desar el PDF." }
                busy=false
            }
        }
    ) { Text("Desa el guió actual com a transcripció PDF") }
    if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
    Text(
        "Aquest PDF conté exactament el text revisat a la pantalla; " +
            "no és reconeixement automàtic de veu. Desa'l després d'editar " +
            "el guió i abans o després de generar el MP3.",
        style=MaterialTheme.typography.bodySmall
    )
}
