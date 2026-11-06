/*
 * LECTAV — first course planning and study progress screens.
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
    onDone: (String, Int, Boolean) -> Unit,
    onGenerate: (String) -> Unit,
    onCurriculum: () -> Unit
) {
    Text("Programació i sessions", style=MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick=onCurriculum,modifier=Modifier.fillMaxWidth()) {
        Text("Consulta el currículum oficial i prepara temari amb IA")
    }
    Text(
        "Proposta didàctica editable. La normativa estableix competències i sabers; " +
        "aquest ordre dels temes no és obligatori ni oficial.",
        style=MaterialTheme.typography.bodySmall
    )
    if(plan==null) {
        if(starterTopicsFor(course).isEmpty()) {
            Text("Encara no hi ha una proposta curricular revisada per a aquesta assignatura. " +
                 "No generarem automàticament un temari suposadament oficial.")
        } else {
            Button(onClick=onCreate, modifier=Modifier.fillMaxWidth()) {
                Text("Crea la proposta inicial de temari")
            }
        }
    } else {
        val total=plan.topics.sumOf { it.sessions }
        val done=plan.topics.sumOf { it.completedSessions.size }
        Text("Progrés registrat en aquest dispositiu: $done de $total sessions",
            style=MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress={if(total==0) 0f else done.toFloat()/total},
            modifier=Modifier.fillMaxWidth()
        )
        plan.topics.forEachIndexed { index, topic ->
            var editTitle by remember(topic.id, topic.title) { mutableStateOf(topic.title) }
            var expanded by remember(topic.id) { mutableStateOf(false) }
            Card(modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Unitat ${index+1}", style=MaterialTheme.typography.labelMedium)
                    if(course.role=="teacher") {
                        OutlinedTextField(
                            value=editTitle, onValueChange={editTitle=it.take(150)},
                            label={Text("Títol de la unitat")},
                            modifier=Modifier.fillMaxWidth()
                        )
                        OutlinedButton(onClick={onRename(topic.id,editTitle)},
                            enabled=editTitle.isNotBlank() && editTitle!=topic.title) {
                            Text("Desa el títol")
                        }
                    } else Text(topic.title,style=MaterialTheme.typography.titleMedium)
                    Text("${topic.completedSessions.size} / ${topic.sessions} sessions")
                    OutlinedButton(onClick={expanded=!expanded}) {
                        Text(if(expanded) "Amaga les sessions" else "Mostra les sessions")
                    }
                    if(expanded) {
                        (1..topic.sessions).forEach { session ->
                            val checked=topic.completedSessions.contains(session)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                if(course.role=="student") Checkbox(
                                    checked=checked,
                                    onCheckedChange={onDone(topic.id,session,it)}
                                )
                                Text("Sessió $session", modifier=Modifier.weight(1f))
                                TextButton(onClick={
                                    onGenerate("${topic.title} — sessió $session")
                                }) {Text("Genera")}
                            }
                        }
                    }
                }
            }
        }
    }
}
