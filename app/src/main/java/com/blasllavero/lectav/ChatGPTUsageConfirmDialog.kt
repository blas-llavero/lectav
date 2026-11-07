package com.blasllavero.lectav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun ChatGPTUsageConfirmDialog(
    @Suppress("UNUSED_PARAMETER") guard: ChatGPTUsageGuard,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var acknowledged by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generació amb ChatGPT") },
        text = {
            Column {
                Text(
                    "LECTAV no limita el nombre de peticions, però ChatGPT " +
                    "aplica la quota del compte. La generació podria " +
                    "consumir crèdits si els has autoritzat al compte."
                )
                Text(
                    "Per impedir l'ús de crèdits addicionals, revisa els " +
                    "límits de l'aplicació a la configuració de ChatGPT. " +
                    "LECTAV no pot veure el teu saldo real."
                )
                OutlinedButton(onClick = {
                    context.startActivity(Intent(
                        Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")
                    ))
                }) { Text("Revisa els límits a ChatGPT") }
                Row {
                    Checkbox(
                        checked = acknowledged,
                        onCheckedChange = { acknowledged = it }
                    )
                    Text(
                        "He revisat la configuració d'ús i crèdits " +
                        "del meu compte i autoritzo aquesta generació."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = acknowledged, onClick = onConfirm) {
                Text("Genera el contingut")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel·la") }
        }
    )
}
