/*
 * LECTAV — user-directed encrypted backup and restore screen.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun BackupScreen(
    settingsRepo: SettingsRepository,
    plansRepo: CoursePlanningRepository,
    library: MaterialLibrary,
    usageGuard: ChatGPTUsageGuard
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    val chatGPTStatus by ChatGPTAuth.state.collectAsState()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedBackup by remember { mutableStateOf<Uri?>(null) }
    var pendingPassword by remember { mutableStateOf("") }
    var pendingLanguage by remember { mutableStateOf("") }
    var pendingSelection by remember { mutableStateOf<BackupSelection?>(null) }

    val saved by settingsRepo.settings.collectAsState(initial=SavedSettings())
    val materials by library.files.collectAsState(initial=emptyList())
    val courses=saved.savedCourses.distinctBy { courseKey(it) }
    val supportedTypes=linkedMapOf(
        "lesson" to "Teoria",
        "exercise" to "Exercicis",
        "solutions" to "Solucions",
        "video" to "Avatar / vídeo",
        "audio" to "Àudio",
        "podcast" to "Podcast",
        "transcript" to "Transcripció"
    )
    var selectedCourses by remember { mutableStateOf<Set<String>?>(null) }
    var selectedTypes by remember { mutableStateOf<Set<String>?>(null) }
    var excludedMaterials by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedCourses by remember { mutableStateOf<Set<String>>(emptySet()) }
    val effectiveCourses=selectedCourses ?: courses.map(::courseKey).toSet()
    val effectiveTypes=selectedTypes ?: supportedTypes.keys
    val candidateFiles=materials.filter { it.courseKey in effectiveCourses
        && it.type in effectiveTypes && it.id !in excludedMaterials }
    val activeSelection=BackupSelection(effectiveCourses,candidateFiles.map { it.id }.toSet())


    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val pass = pendingPassword
            val locale = pendingLanguage
            val selection = pendingSelection
            pendingPassword = ""
            pendingSelection = null
            busy = true
            error = ""
            scope.launch {
                val result = runCatching {
                    LectavBackup.export(
                        context, uri, pass, settingsRepo, plansRepo, library,
                        usageGuard, locale, selection
                    )
                }
                result.onSuccess {
                    message = "Export verified: " + it.courses + " courses and " +
                        it.files + " actual material files (" + it.bytes +
                        " bytes). Save the password. " +
                        "Before uninstalling, restore this archive on a test device."
                }.onFailure {
                    error = it.localizedMessage ?: "No s'ha pogut crear la còpia."
                    // A failed archive may leave an incomplete destination file.
                    message = "Si ha aparegut un fitxer incomplet, elimina'l del destí."
                }
                busy = false
            }
        }
        pendingPassword = ""
    }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedBackup = uri
            restorePassword = password
            error = ""
            message = ""
        }
    }

    Text("Còpia de seguretat de LECTAV", style = MaterialTheme.typography.titleLarge)
    Text(
        "Exporta i restaura la configuració, els cursos, les programacions, " +
        "el progrés, els PDF i els vídeos accessibles de la biblioteca. " +
        "Selecciona una carpeta interna compartida o de la targeta SD. " +
        "No es puja res a GitHub.",
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        "Important: la sessió i els tokens de ChatGPT NO es poden copiar " +
        "de manera segura entre instal·lacions. Es conserva la pista del " +
        "compte i les preferències del generador, però després de restaurar " +
        "hauràs d'iniciar sessió de nou.",
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        "L'arxiu de còpia estarà xifrat amb una contrasenya de 10 caràcters " +
        "o més. Si l'oblides, no podràs recuperar els materials. " +
        "En restaurar, podràs introduir-la després de seleccionar el fitxer.",
        style = MaterialTheme.typography.bodySmall
    )

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = password, onValueChange = { password = it.take(200) },
        visualTransformation = PasswordVisualTransformation(),
        label = { Text("Contrasenya de la còpia (mínim 10 caràcters)") },
        singleLine = true
    )
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = confirmPassword, onValueChange = { confirmPassword = it.take(200) },
        visualTransformation = PasswordVisualTransformation(),
        label = { Text("Repeteix la contrasenya per exportar") },
        singleLine = true
    )
    HorizontalDivider()
    Text("Selecciona cursos, assignatures i materials",
        style=MaterialTheme.typography.titleMedium)
    Text("Pots exportar tota la biblioteca o només determinats cursos, " +
        "assignatures, tipus de material i fitxers individuals. " +
        "Les còpies parcials no substitueixen els altres cursos en importar-les.",
        style=MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            enabled=!busy,
            onClick={
                selectedCourses=null
                selectedTypes=null
                excludedMaterials=emptySet()
            }
        ){Text("Selecciona-ho tot")}
        OutlinedButton(
            enabled=!busy,
            onClick={ selectedCourses=emptySet() }
        ){Text("Cap curs")}
    }
    Text("Tipus de contingut",style=MaterialTheme.typography.titleSmall)
    supportedTypes.forEach { (type,display) ->
        val checked=type in effectiveTypes
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked=checked,enabled=!busy,
                onCheckedChange={ value ->
                    val next=effectiveTypes.toMutableSet()
                    if(value)next.add(type) else next.remove(type)
                    selectedTypes=next
                }
            )
            Text(display)
        }
    }
    Text("Cursos i assignatures",style=MaterialTheme.typography.titleSmall)
    if(courses.isEmpty())Text("Encara no hi ha cap curs desat.")
    courses.forEach { course ->
        val key=courseKey(course)
        val checked=key in effectiveCourses
        val files=materials.filter { it.courseKey==key }
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked=checked,enabled=!busy,
                onCheckedChange={ value ->
                    val next=effectiveCourses.toMutableSet()
                    if(value)next.add(key) else next.remove(key)
                    selectedCourses=next
                }
            )
            Text(
                "${course.pathway.uppercase(Locale.ROOT)} · ${course.course} · ${course.subject}",
                modifier=Modifier.weight(1f)
            )
            TextButton(onClick={
                expandedCourses=if(key in expandedCourses)expandedCourses-key
                    else expandedCourses+key
            }){Text(if(key in expandedCourses)"Amaga" else "Elements (${files.size})")}
        }
        if(key in expandedCourses){
            if(files.isEmpty())Text("Sense materials desats.",
                style=MaterialTheme.typography.bodySmall)
            files.forEach { file ->
                val active=checked && file.type in effectiveTypes
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
                    modifier=Modifier.padding(start=22.dp)) {
                    Checkbox(
                        checked=active && file.id !in excludedMaterials,
                        enabled=!busy && active,
                        onCheckedChange={ value ->
                            excludedMaterials=if(value)excludedMaterials-file.id
                                else excludedMaterials+file.id
                        }
                    )
                    Text("${supportedTypes[file.type] ?: file.type}: ${file.name}",
                        style=MaterialTheme.typography.bodySmall,
                        modifier=Modifier.weight(1f))
                }
            }
        }
    }
    Text("Selected for export: ${effectiveCourses.size} subjects and " +
        "${candidateFiles.size} material files.",
        style=MaterialTheme.typography.bodyMedium)
    if(materials.isEmpty()) {
        Text("WARNING: This installation has no indexed PDFs, audio, " +
            "podcasts or videos. A backup created here will contain " +
            "settings but no documents. Check the course library " +
            "before exporting or reinstalling.",
            color=MaterialTheme.colorScheme.error)
    }
    if(materials.isNotEmpty() && candidateFiles.isEmpty()) {
        Text("WARNING: Your current selection contains zero files. " +
            "Choose Select all or include the specific courses and file types.",
            color=MaterialTheme.colorScheme.error)
    }

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy && password.length >= 10 && password == confirmPassword
            && effectiveCourses.isNotEmpty(),
        onClick = {
            error = ""; message = ""
            pendingPassword = password
            pendingLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            pendingSelection = activeSelection
            val today = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ROOT).format(Date())
            exportPicker.launch("LECTAV-seleccio-" + today + ".lectavbak")
        }
    ) { Text("Exporta només la selecció…") }
    OutlinedButton(
        modifier=Modifier.fillMaxWidth(),
        enabled=!busy && password.length>=10 && password==confirmPassword,
        onClick={
            error="";message=""
            pendingPassword=password
            pendingLanguage=AppCompatDelegate.getApplicationLocales().toLanguageTags()
            pendingSelection=null
            val today=SimpleDateFormat("yyyy-MM-dd_HH-mm",Locale.ROOT).format(Date())
            exportPicker.launch("LECTAV-backup-complet-"+today+".lectavbak")
        }
    ){Text("Exporta-ho tot (configuració + cursos + materials)…")}

    HorizontalDivider()
    Text("Restaura una còpia anterior", style = MaterialTheme.typography.titleMedium)
    Text(
        "Còpia completa: substitueix els cursos i materials locals. " +
        "Còpia selectiva: afegeix o actualitza només els cursos i materials " +
        "inclòsos en el fitxer; els altres cursos es conserven. " +
        "No desinstal·lis LECTAV abans de comprovar la còpia.",
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy,
        onClick = { error = ""; message = ""; importPicker.launch(arrayOf("*/*")) }
    ) { Text("Selecciona un fitxer .lectavbak…") }

    if (selectedBackup != null) {
        AlertDialog(
            onDismissRequest = {
                selectedBackup = null
                restorePassword = ""
            },
            title = { Text("Confirma la restauració") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Es verificaran la contrasenya i els documents abans de " +
                        "substituir la configuració i la biblioteca actuals. " +
                        "Torna a iniciar sessió amb ChatGPT després."
                    )
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it.take(200) },
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("Contrasenya del fitxer seleccionat") },
                        supportingText = {
                            Text("Introdueix la contrasenya de la còpia (mínim 10 caràcters).")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (chatGPTStatus.connected) {
                        Text(
                            "Abans de restaurar, desconnecta el compte ChatGPT " +
                                "des de Configuració → Compte ChatGPT. " +
                                "Això evita substituir dades mentre hi ha una sessió activa.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && restorePassword.length >= 10 && !chatGPTStatus.connected,
                    onClick = {
                    val uri = selectedBackup!!
                    selectedBackup = null
                    val pass = restorePassword
                    restorePassword = ""
                    busy = true
                    error = ""; message = ""
                    scope.launch {
                        val result = runCatching {
                            LectavBackup.restore(
                                context, uri, pass, settingsRepo, plansRepo,
                                library, usageGuard
                            )
                        }
                        result.onSuccess {
                            // Non-blocking, temporary confirmation: no Accept button.
                            Toast.makeText(
                                context,
                                "Importació completada: ${it.courses} cursos i ${it.files} materials.",
                                Toast.LENGTH_LONG
                            ).show()
                            message = "Restore verified: " + it.courses +
                                " courses and " + it.files +
                                " readable material files. Open your course " +
                                "library to inspect the restored PDFs and audio. " + + (if (it.accountHint.isNotBlank())
                                    "Compte anterior: " + it.accountHint + ". "
                                else "") + "Connecta ChatGPT de nou."
                            if(it.languageTag.isNotBlank()) {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(it.languageTag)
                                )
                            }
                        }.onFailure {
                            error = it.localizedMessage
                                ?: "No s'ha pogut restaurar la còpia."
                        }
                        busy = false
                    }
                }) { Text("Restaura") }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedBackup = null
                    restorePassword = ""
                }) {
                    Text("Cancel·la")
                }
            }
        )
    }
    if (busy) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text("Processant arxiu xifrat i materials…")
    }
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium)
    Text(
        "Mida màxima per còpia: 1 GB de materials. Si tens MP4 molt grans, " +
        "exporta'ls individualment a la SD. " +
        "No es recuperen automàticament fitxers d'una instal·lació " +
        "ja desinstal·lada sense còpia prèvia.",
        style = MaterialTheme.typography.bodySmall
    )
}
