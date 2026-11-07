package com.blasllavero.lectav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun ChatGPTUsageControls(@Suppress("UNUSED_PARAMETER") guard: ChatGPTUsageGuard) {
    val context = LocalContext.current
    HorizontalDivider()
    Text("Ús de ChatGPT", style = MaterialTheme.typography.titleMedium)
    Text(
        "LECTAV ja no imposa un límit local d'intents de generació. " +
        "Pots crear nous documents mentre el teu compte ChatGPT tingui accés " +
        "disponible. Els límits reals del pla continuen aplicant-se.",
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        "Si has activat l'ús de crèdits o compres automàtiques a ChatGPT, " +
        "les peticions podrien consumir saldo. LECTAV no pot consultar " +
        "l'import restant ni activar un pagament sense el teu consentiment. " +
        "Revisa Configuració → Ús → Límits de les aplicacions.",
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")))
        }
    ) { Text("Obre ChatGPT per revisar ús i crèdits") }
    HorizontalDivider()
}
