/*
 * LECTAV — trimester programme and individually named lessons.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CoursePlanScreen(
    course: SavedCourse,
    plan: CoursePlan?,
    onCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onSessionTitle: (String, Int, String) -> Unit,
    onDone: (String, Int, Boolean) -> Unit,
    onGenerate: (String) -> Unit,
    onCurriculum: () -> Unit
) {
    Text("Programació didàctica i sessions", style=MaterialTheme.typography.titleLarge)
    Button(onClick=onCurriculum,modifier=Modifier.fillMaxWidth()) {
        Text("Genera un trimestre amb ChatGPT")
    }
    Text(
        "Selecciona el trimestre i el nombre de sessions. ChatGPT " +
        "proposarà les unitats i un tema específic per a cada sessió. " +
        "Les propostes són editables, no són un ordre fixat per la llei. " +
        "No s'utilitzarà ChatGPT sense el teu consentiment i límit d'ús.",
        style=MaterialTheme.typography.bodySmall
    )
    if(plan == null || plan.topics.isEmpty()) {
        Text(
            "Encara no hi ha una programació desada per a aquesta assignatura. " +
            "Prem «Genera un trimestre amb ChatGPT»; no cal un temari precarregat.",
            style=MaterialTheme.typography.bodyMedium
        )
    } else {
        val total=plan.topics.sumOf { it.sessions }
        val done=plan.topics.sumOf { it.completedSessions.size }
        Text("Progrés local: $done de $total sessions",style=MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress={if(total==0) 0f else done.toFloat()/total},
            modifier=Modifier.fillMaxWidth()
        )
        (1..3).forEach { trimester ->
            val quarterTopics=plan.topics.filter { it.term==trimester }
            Text("Trimestre $trimester",style=MaterialTheme.typography.titleMedium)
            if (quarterTopics.isEmpty()) {
                Text("Pendent de generar",style=MaterialTheme.typography.bodySmall)
            }
            var absoluteNumber=0
            quarterTopics.forEachIndexed { index, topic ->
                var editTitle by remember(topic.id, topic.title) { mutableStateOf(topic.title) }
                var expanded by remember(topic.id) { mutableStateOf(false) }
                Card(modifier=Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement=Arrangement.spacedBy(7.dp)
                    ) {
                        Text("Unitat ${index+1} · ${topic.sessions} sessions",
                            style=MaterialTheme.typography.labelMedium)
                        if(course.role=="teacher") {
                            OutlinedTextField(
                                value=editTitle,
                                onValueChange={editTitle=it.take(150)},
                                label={Text("Títol de la unitat")},
                                modifier=Modifier.fillMaxWidth()
                            )
                            if(editTitle.isNotBlank() && editTitle!=topic.title) {
                                OutlinedButton(onClick={onRename(topic.id,editTitle)}) {
                                    Text("Desa el títol")
                                }
                            }
                        } else Text(topic.title,style=MaterialTheme.typography.titleMedium)
                        Text("${topic.completedSessions.size} / ${topic.sessions} completades",
                            style=MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick={expanded=!expanded}) {
                            Text(if(expanded) "Plega les sessions" else "Mostra les sessions")
                        }
                        (1..topic.sessions).forEach { localNumber ->
                            absoluteNumber++
                            if(expanded) {
                                val title=topic.sessionTitles
                                    .getOrNull(localNumber-1)?.takeIf { it.isNotBlank() }
                                    ?: "${topic.title} (sessió $localNumber)"
                                var edited by remember(topic.id,localNumber,title) {
                                    mutableStateOf(title)
                                }
                                Text("Sessió $absoluteNumber · $title",
                                    style=MaterialTheme.typography.bodyMedium)
                                if(course.role=="teacher" &&
                                    topic.sessionTitles.size==topic.sessions) {
                                    OutlinedTextField(
                                        value=edited,
                                        onValueChange={edited=it.take(180)},
                                        label={Text("Tema d'aquesta sessió")},
                                        modifier=Modifier.fillMaxWidth(),
                                        singleLine=false,
                                        maxLines=2
                                    )
                                    if(edited.trim()!=title && edited.trim().length >= 3) {
                                        OutlinedButton(onClick={
                                            onSessionTitle(topic.id,localNumber,edited)
                                        }) { Text("Desa tema de la sessió") }
                                    }
                                }
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    if(course.role=="student") {
                                        val checked=topic.completedSessions.contains(localNumber)
                                        Checkbox(
                                            checked=checked,
                                            onCheckedChange={onDone(topic.id,localNumber,it)}
                                        )
                                    }
                                    TextButton(onClick={onGenerate(title)}) {
                                        Text("Genera materials de la sessió")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
