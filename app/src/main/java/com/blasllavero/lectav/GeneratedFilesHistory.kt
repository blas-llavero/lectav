/*
 * LECTAV — live generated-material history with non-fabricated dates.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The list is derived from MaterialLibrary's observable DataStore index.
 * It refreshes as soon as the repository finishes saving a file. Original
 * timestamps are kept through encrypted backups; older versions have no
 * reliable creation dates and are identified as such.
 */
@Composable
internal fun GeneratedFilesHistory(
    files: List<LocalMaterial>,
    courses: List<SavedCourse>,
    plans: List<CoursePlan>
) {
    val generated = files.asSequence()
        .filter { it.origin == "generated" }
        .sortedWith(compareBy<LocalMaterial> { it.createdAt }.thenBy { it.name })
        .toList()
    HorizontalDivider()
    Text("Fitxers generats", style = MaterialTheme.typography.titleMedium)
    Text(
        "Aquest historial s'actualitza després de desar cada material. " +
            "Els fitxers més recents apareixen a la part inferior.",
        style = MaterialTheme.typography.bodySmall
    )
    if(generated.isEmpty()) {
        Text("Encara no s'ha generat cap fitxer.")
        return
    }
    Text(generated.size.toString() + " fitxers generats i conservats a la biblioteca.",
        style=MaterialTheme.typography.labelMedium)
    if(generated.size > 80) {
        Text("Es mostren els últims 80. La biblioteca conserva la resta.",
            style=MaterialTheme.typography.bodySmall)
    }
    val last=generated.takeLast(80)
    val formatter=SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())
    last.forEach { file ->
        val course=courses.firstOrNull { courseKey(it)==file.courseKey }
        val plan=plans.firstOrNull { it.courseKey==file.courseKey }
        val session=file.sessionKey.split('|')
        val topic=if(session.size==3 && plan!=null) {
            val unit=plan.topics.firstOrNull {
                it.id==session[1] && it.trimester.toString()==session[0]
            }
            val number=session[2].toIntOrNull()
            if(unit!=null && number!=null && number in 1..unit.sessions)
                unit.sessionTitle(number)
            else null
        } else null
        val subject=course?.let { c ->
            catalogueSubjectsFor(c.course).firstOrNull { it.id==c.subject }
                ?.officialNameCa ?: c.subject
        } ?: file.courseKey.substringAfterLast('|',"Assignatura desconeguda")
        val level=course?.course ?: file.courseKey.split('|').getOrNull(2)
            ?: "Curs no identificat"
        val date=if(file.createdAt>0L)
            formatter.format(Date(file.createdAt))
        else "Data no registrada (versió anterior)"
        Card(modifier=Modifier.fillMaxWidth()) {
            Column(modifier=Modifier.padding(12.dp),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(file.name, style=MaterialTheme.typography.titleSmall)
                Text(date, style=MaterialTheme.typography.bodySmall)
                Text("Curs: "+level+" · Assignatura: "+subject,
                    style=MaterialTheme.typography.bodySmall)
                Text("Tema: "+(topic ?: "Sense sessió vinculada"),
                    style=MaterialTheme.typography.bodySmall)
                Text("Contingut: "+AutoGenerationConfig.title(file.type),
                    style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
