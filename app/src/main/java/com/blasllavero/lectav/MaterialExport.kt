/*
 * LECTAV — explicit user-selected local/SD export without broad storage access.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * A destination URI supplied by Android Documents is not necessarily an
 * absolute filesystem path. Only external-storage document IDs can be
 * described as a recognizable volume/folder without misleading the user.
 */
internal object MaterialLocation {
    fun source(context: Context, material: LocalMaterial): String {
        if (material.origin == "generated" || material.origin == "restored") {
            return (if (material.origin == "restored")
                "Material recuperat d'una còpia: carpeta privada de LECTAV\n"
            else "Original privat: memòria interna de LECTAV\n") +
                context.filesDir.absolutePath + "/generated/\n" +
                "Aquesta carpeta està protegida per Android i, sense accés especial, " +
                "CX File Explorer no la pot obrir."
        }
        return "Fitxer importat: es conserva a la ubicació original.\n" +
            selectedLocation(material.uri)
    }

    fun selectedLocation(rawUri: String): String {
        val uri = Uri.parse(rawUri)
        if (uri.authority == "com.android.externalstorage.documents") {
            val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            if (!id.isNullOrBlank() && ':' in id) {
                val volume = id.substringBefore(':')
                val relative = id.substringAfter(':').trimStart('/')
                val label = if (volume == "primary")
                    "Memòria interna compartida"
                else "Targeta SD o volum extern ($volume)"
                return "$label / $relative"
            }
        }
        // Other providers (Google Drive, Downloads, media, manufacturer
        // documents apps) do not promise a meaningful filesystem path.
        return "Document accessible des del selector de fitxers d'Android " +
            "(la ruta física no està disponible per a aquest proveïdor)."
    }
}

internal fun materialMime(kind: String): String =
    if (kind == "video") "video/mp4" else "application/pdf"

/**
 * This component is present only when a user explicitly chooses to export.
 * ACTION_CREATE_DOCUMENT shows the Android folder picker on internal storage,
 * SD, or other installed document providers. Originals remain untouched.
 */
@Composable
internal fun ExportDocumentButton(
    sourceUri: String,
    displayName: String,
    mime: String,
    onExported: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copying by remember(sourceUri) { mutableStateOf(false) }
    var message by remember(sourceUri) { mutableStateOf("") }
    var exportedUri by remember(sourceUri) { mutableStateOf("") }

    val create = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mime)
    ) { destination ->
        if (destination != null) {
            copying = true
            message = ""
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        copyDocument(context, Uri.parse(sourceUri), destination)
                    }
                }
                result.onSuccess {
                    exportedUri = destination.toString()
                    onExported(destination.toString())
                    message = "Còpia desada correctament a la destinació escollida. " +
                        "Si has triat emmagatzematge local o SD, podràs " +
                        "trobar-la a CX File Explorer. Si has triat un " +
                        "proveïdor al núvol, el fitxer pot sincronitzar-s'hi."
                }.onFailure { error ->
                    message = "No s'ha pogut desar la còpia: " +
                        (error.localizedMessage ?: "comprova l'espai lliure i els permisos.")
                    // Only the newly created destination is considered. Never
                    // delete the source material or an existing imported file.
                    runCatching {
                        withContext(Dispatchers.IO) {
                            DocumentsContract.deleteDocument(context.contentResolver, destination)
                        }
                    }
                }
                copying = false
            }
        }
    }
    OutlinedButton(
        onClick = {
            message = ""
            val safeName = displayName
                .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_")
                .take(140)
                .ifBlank { "LECTAV" }
            val ext = if (mime == "video/mp4") ".mp4" else ".pdf"
            create.launch(if (safeName.endsWith(ext, true)) safeName else safeName + ext)
        },
        enabled = !copying,
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (copying) "Desant còpia…" else "Desa una còpia a una carpeta…") }
    Text(
        "Tria una carpeta de la memòria interna o de la targeta SD " +
            "per conservar una còpia accessible. Si esculls un servei al núvol " +
            "al selector d'Android, la còpia pot enviar-se a aquell servei.",
        style = MaterialTheme.typography.bodySmall
    )
    if (copying) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
    if (exportedUri.isNotBlank()) {
        Text(
            "Darrera destinació: " + MaterialLocation.selectedLocation(exportedUri),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun copyDocument(context: Context, source: Uri, destination: Uri) {
    require(source != destination) { "Origen i destinació no poden ser iguals." }
    val resolver = context.contentResolver
    val input = resolver.openInputStream(source)
        ?: throw IOException("No es pot llegir el fitxer original.")
    input.use { stream ->
        val output = resolver.openOutputStream(destination, "wt")
            ?: throw IOException("No es pot escriure a la carpeta seleccionada.")
        output.use { target ->
            val copied = stream.copyTo(target, bufferSize = 64 * 1024)
            if (copied <= 0L) throw IOException("El document original és buit.")
            target.flush()
        }
    }
}
