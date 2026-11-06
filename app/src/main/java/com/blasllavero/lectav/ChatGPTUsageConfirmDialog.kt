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
    guard: ChatGPTUsageGuard,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val usage by guard.state.collectAsState()
    var checked by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abans de generar amb ChatGPT") },
        text = {
            Column {
                Text(
                    "LECTAV farà una petició amb el teu pla ChatGPT. " +
                    "Resten ${usage.remaining} intents segons el teu límit LOCAL. " +
                    "Això no és el saldo real de ChatGPT."
                )
                Text(
                    "Si l'ús de crèdits està activat al compte, " +
                    "una petició podria consumir-ne. LECTAV no pot " +
                    "consultar ni bloquejar directament aquest saldo."
                )
                OutlinedButton(onClick = {
                    context.startActivity(Intent(
                        Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")
                    ))
                }) { Text("Revisa la quota i els crèdits a ChatGPT") }
                Row {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Text(
                        "He comprovat que el límit de LECTAV a ChatGPT és " +
                        "inferior al 100% i que no vull utilitzar crèdits " +
                        "ni compres automàtiques per aquesta petició."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = checked && !usage.blocked, onClick = onConfirm) {
                Text("Genera amb la quota del meu pla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel·la") }
        }
    )
}
