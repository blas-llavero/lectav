/*
 * LECTAV — indexed three-term, teacher-editable lessons and learner progress.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
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
    onRename: (String, String) -> Unit,
    onRenameSession: (String, Int, String) -> Unit,
    onDone: (String, Int, Boolean) -> Unit,
    onGenerate: (String) -> Unit,
    onCurriculum: () -> Unit,
    onAiProgramme: () -> Unit
) {
    Text("Programació didàctica · Índex de sessions",
        style = MaterialTheme.typography.titleLarge)
    Text(
        "Tres trimestres, unitats i títol concret de cada sessió. " +
        "És una seqüenciació editable, no l'ordre obligatori d'una llei.",
        style = MaterialTheme.typography.bodySmall
    )
    if (course.role == "teacher") {
        Button(onClick = onAiProgramme, modifier = Modifier.fillMaxWidth()) {
            Text("Genera una programació trimestral amb ChatGPT")
        }
    }
    OutlinedButton(onClick = onCurriculum, modifier = Modifier.fillMaxWidth()) {
        Text("Consulta el currículum oficial de la matèria")
    }
    if (plan == null || plan.topics.isEmpty()) {
        Text(
            if (course.role == "teacher")
                "Encara no hi ha cap programació. Selecciona un trimestre i " +
                "demana a ChatGPT que proposi les unitats i les sessions, " +
                "sense continguts precarregats."
            else "Aquest curs encara no té cap programació desada en " +
                "aquest dispositiu.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    val total = plan.topics.sumOf { it.sessions }
    val done = plan.topics.sumOf { it.completedSessions.size }
    Text("Progrés: $done de $total sessions", style = MaterialTheme.typography.titleMedium)
    LinearProgressIndicator(
        progress = { if (total == 0) 0f else done.toFloat() / total },
        modifier = Modifier.fillMaxWidth()
    )

    for (term in 1..3) {
        val units = plan.topics.filter { it.trimester == term }
        val title = when (term) {
            1 -> "Primer trimestre"
            2 -> "Segon trimestre"
            else -> "Tercer trimestre"
        }
        HorizontalDivider()
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (units.isEmpty()) {
            Text("Encara no s'han generat unitats per a aquest trimestre.",
                style = MaterialTheme.typography.bodySmall)
        }
        units.forEachIndexed { unitIndex, unit ->
            val trimesterSessionOffset = units.take(unitIndex).sumOf { it.sessions }
            var titleEdit by remember(unit.id,unit.title) { mutableStateOf(unit.title) }
            var expanded by remember(unit.id) { mutableStateOf(false) }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Unitat ${unitIndex + 1} · ${unit.title}",
                        style = MaterialTheme.typography.titleSmall)
                    if (course.role == "teacher") {
                        OutlinedTextField(
                            value = titleEdit,
                            onValueChange = { titleEdit = it.take(150) },
                            label = { Text("Nom de la unitat") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (titleEdit.trim() != unit.title) {
                            OutlinedButton(
                                enabled = titleEdit.trim().length >= 3,
                                onClick = { onRename(unit.id, titleEdit) }
                            ) { Text("Actualitza el nom de la unitat") }
                        }
                    }
                    Text("${unit.completedSessions.size} de ${unit.sessions} sessions completades")
                    OutlinedButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Amaga l'índex de sessions"
                            else "Mostra l'índex de sessions")
                    }
                    if (expanded) {
                        (1..unit.sessions).forEach { number ->
                            val lesson = unit.sessionTitle(number)
                            var editing by remember(unit.id,number) { mutableStateOf(false) }
                            var edited by remember(unit.id,number,lesson) {
                                mutableStateOf(lesson)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Sessió ${trimesterSessionOffset + number} · $lesson",
                                    style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (course.role == "student") {
                                        Checkbox(
                                            checked = number in unit.completedSessions,
                                            onCheckedChange = {
                                                onDone(unit.id, number, it)
                                            }
                                        )
                                    }
                                    TextButton(onClick = {
                                        onGenerate(
                                            "${unit.title} — Sessió ${trimesterSessionOffset + number}: $lesson"
                                        )
                                    }) { Text("Genera materials") }
                                    if (course.role == "teacher") {
                                        TextButton(onClick = { editing = !editing }) {
                                            Text(if (editing) "Tanca" else "Edita el tema")
                                        }
                                    }
                                }
                                if (course.role == "teacher" && editing) {
                                    OutlinedTextField(
                                        value = edited,
                                        onValueChange = { edited = it.take(150) },
                                        label = { Text("Tema que s'impartirà") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3
                                    )
                                    OutlinedButton(
                                        enabled = edited.trim().length >= 3,
                                        onClick = {
                                            onRenameSession(unit.id,number,edited)
                                            editing = false
                                        }
                                    ) { Text("Desa el tema de la sessió") }
                                }
                            }
                            if (number != unit.sessions) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
