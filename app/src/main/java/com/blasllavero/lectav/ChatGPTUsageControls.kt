package com.blasllavero.lectav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatGPTUsageControls(guard: ChatGPTUsageGuard) {
    val usage by guard.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { guard.refresh() }
    HorizontalDivider()
    Text("Control d'ús de ChatGPT", style = MaterialTheme.typography.titleMedium)
    Text(
        "Intents de generació des d'aquest dispositiu aquesta setmana: " +
        "${usage.attempts} de ${usage.weeklyLimit}; restants: ${usage.remaining}.",
        style = MaterialTheme.typography.bodyMedium
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(enabled = usage.weeklyLimit > 0, onClick = {
            guard.setWeeklyLimit(usage.weeklyLimit - 1)
        }) { Text("−") }
        Text("Màxim: ${usage.weeklyLimit} per setmana", modifier = Modifier.weight(1f))
        OutlinedButton(enabled = usage.weeklyLimit < 30, onClick = {
            guard.setWeeklyLimit(usage.weeklyLimit + 1)
        }) { Text("+") }
    }
    Text(
        "És un límit LOCAL d'intents, no un comptador real de quota, " +
        "tokens ni diners. Els errors també compten. Valor 0: generació " +
        "ChatGPT desactivada a LECTAV. El recompte es reinicia els dilluns.",
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        "Per evitar consumir crèdits, a ChatGPT ves a Configuració → Ús → " +
        "Límits de les aplicacions → LECTAV → Gestiona els límits, " +
        "i estableix un límit inferior al 100%. Revisa també els " +
        "crèdits autoritzats i les compres automàtiques. " +
        "LECTAV no pot llegir aquests ajustos del teu compte.",
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")))
    }) { Text("Obre ChatGPT per revisar l'ús i els crèdits") }
    HorizontalDivider()
}
