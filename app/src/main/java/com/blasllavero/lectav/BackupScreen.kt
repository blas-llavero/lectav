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

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val pass = pendingPassword
            val locale = pendingLanguage
            pendingPassword = ""
            busy = true
            error = ""
            scope.launch {
                val result = runCatching {
                    LectavBackup.export(
                        context, uri, pass, settingsRepo, plansRepo, library,
                        usageGuard, locale
                    )
                }
                result.onSuccess {
                    message = "Còpia desada: " + it.courses + " cursos i " +
                        it.files + " materials. Conserva la contrasenya."
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
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy && password.length >= 10 && password == confirmPassword,
        onClick = {
            error = ""; message = ""
            pendingPassword = password
            pendingLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val today = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ROOT).format(Date())
            exportPicker.launch("LECTAV-backup-" + today + ".lectavbak")
        }
    ) { Text("Exporta configuració i materials…") }

    HorizontalDivider()
    Text("Restaura una còpia anterior", style = MaterialTheme.typography.titleMedium)
    Text(
        "La restauració REEMPLAÇARÀ les llistes de cursos, els plans i els " +
        "materials indexats d'aquesta instal·lació. " +
        "No desinstal·lis LECTAV abans d'haver comprovat que la còpia existeix.",
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
                            message = "Restauració completada: " + it.courses +
                                " cursos i " + it.files +
                                " materials. " + (if (it.accountHint.isNotBlank())
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
        "Mida màxima per còpia: 250 MB de materials. Si tens MP4 molt grans, " +
        "exporta'ls individualment a la SD. " +
        "No es recuperen automàticament fitxers d'una instal·lació " +
        "ja desinstal·lada sense còpia prèvia.",
        style = MaterialTheme.typography.bodySmall
    )
}
