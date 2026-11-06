/*
 * LECTAV — optional per-user ChatGPT sign-in controls.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatGPTAccountScreen(
    status: ChatGPTAuthState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Text("Compte ChatGPT", style = MaterialTheme.typography.titleLarge)
    Text(
        "La connexió amb ChatGPT és opcional. Els teus cursos, documents i " +
        "el registre local continuen disponibles sense compte.",
        style = MaterialTheme.typography.bodyMedium
    )
    HorizontalDivider()
    if (!status.connected) {
        Text(
            "Si tens un compte compatible, pots autoritzar LECTAV a generar " +
            "teoria, programacions, exercicis i solucionaris. El navegador " +
            "d'OpenAI gestionarà l'accés: LECTAV no et demanarà contrasenyes " +
            "ni claus API.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            enabled = !status.busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSignIn
        ) { Text("Continua amb ChatGPT") }
    } else {
        Text(
            "Compte: " + status.email.ifBlank { "Identitat validada" },
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            if (status.inferenceAllowed)
                "Has autoritzat la generació amb el teu pla ChatGPT. " +
                "Els models disponibles es consultaran quan obris el generador."
            else "Has iniciat sessió, però falta autoritzar l'ús del pla " +
                "per generar continguts.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (!status.inferenceAllowed) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !status.busy,
                onClick = onSignIn
            ) { Text("Autoritza la generació amb ChatGPT") }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !status.busy,
            onClick = onSignOut
        ) { Text("Desconnecta ChatGPT") }
    }
    if (status.busy) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            "Completa el procés al navegador. Quan acabis, torna a LECTAV.",
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (status.message.isNotBlank()) {
        Text(status.message, style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider()
    Text(
        "Privacitat: la sessió es desa xifrada al dispositiu. Quan utilitzes " +
        "ChatGPT per generar un document, el tema i la petició s'envien " +
        "a OpenAI. No hi incorporis dades personals d'alumnes ni " +
        "informació confidencial. Els documents només es guarden " +
        "a LECTAV quan tu ho confirmes.",
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        "Els límits d'ús depenen del compte i del pla. Els vídeos de " +
        "l'avatar no es generen amb aquesta connexió: necessitaran " +
        "un motor audiovisual separat.",
        style = MaterialTheme.typography.bodySmall
    )
}
