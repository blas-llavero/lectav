/*
 * LECTAV — official sources are read locally, not used as ready-made curricula.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
internal fun CurriculumSourceScreen(
    course: SavedCourse,
    courseTitle: String,
    subjectTitle: String,
    hasExistingPlan: Boolean,
    chatGPTStatus: ChatGPTAuthState,
    usageGuard: ChatGPTUsageGuard,
    onChatGPT: () -> Unit,
    onApplyPlan: (List<TopicPlan>) -> Unit
) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var officialUrl by rememberSaveable(course.id) {
        mutableStateOf(CurriculumDocumentLoader.initialUrl(course))
    }
    var source by remember(course.id) {
        mutableStateOf(CurriculumDocumentLoader.cached(context,course))
    }
    var loading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Text("Currículum oficial sota demanda",style=MaterialTheme.typography.titleLarge)
    Text("$courseTitle · $subjectTitle",style=MaterialTheme.typography.titleMedium)
    Text(
        "LECTAV llegeix el PDF oficial al dispositiu i conserva l'enllaç, " +
        "la data i l'empremta digital. El temari i les sessions es " +
        "proposen després amb ChatGPT al generador trimestral; " +
        "la seqüència no és fixada per la legislació.",
        style=MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        value=officialUrl,onValueChange={officialUrl=it.take(700)},
        label={Text("Enllaç HTTPS al PDF oficial")},
        modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=3
    )
    if(course.pathway=="eso") {
        OutlinedButton(onClick={
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://xtec.gencat.cat/ca/curriculum/eso/curriculum-175-2022/")))
        },modifier=Modifier.fillMaxWidth()) {
            Text("Índex oficial del currículum d'ESO (XTEC)")
        }
    }
    Text(
        "Les fonts oficials disponibles inicialment són Matemàtiques " +
        "i Biologia i Geologia d'ESO. Per altres matèries pots " +
        "introduir manualment un PDF d'un portal oficial admès. " +
        "Cal comprovar-ne la vigència i l'adequació al curs.",
        style=MaterialTheme.typography.bodySmall
    )
    Button(
        enabled=!loading && officialUrl.isNotBlank(),
        onClick={
            loading=true;error=""
            scope.launch {
                val result=runCatching {
                    CurriculumDocumentLoader.initialize(context)
                    CurriculumDocumentLoader.retrieve(officialUrl)
                }
                result.onSuccess {
                    if(CurriculumDocumentLoader.save(context,course,it)) source=it
                    else error="No s'ha pogut conservar el currículum."
                }.onFailure {
                    error=it.localizedMessage ?: "No s'ha pogut consultar el PDF."
                }
                loading=false
            }
        },modifier=Modifier.fillMaxWidth()
    ) { Text(if(loading) "Descarregant el PDF oficial…" else "Actualitza el currículum oficial") }
    if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
    source?.let { file ->
        HorizontalDivider()
        Text("Font consultada",style=MaterialTheme.typography.titleMedium)
        Text(file.sourceUrl,style=MaterialTheme.typography.bodySmall)
        Text(
            "Descarregada: " +
                DateFormat.getDateTimeInstance().format(Date(file.fetchedAt)) +
                " · ${file.totalPages} pàgines",
            style=MaterialTheme.typography.bodySmall
        )
        Text("SHA-256: ${file.sourceHash.take(30)}…",
            style=MaterialTheme.typography.bodySmall)
        Text(
            "S'ha extret un fragment del document; no s'ha comprovat " +
            "automàticament tota la normativa ni les esmenes posteriors.",
            style=MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick={expanded=!expanded}) {
            Text(if(expanded) "Amaga l'extracte" else "Mostra l'extracte")
        }
        if(expanded) Text(file.extractedText.take(3500),
            style=MaterialTheme.typography.bodySmall)
        Text(
            "Torna a «Programació trimestral» per generar unitats i " +
            "títols de sessió amb ChatGPT a partir d'aquesta font.",
            style=MaterialTheme.typography.bodyMedium
        )
    }
}
