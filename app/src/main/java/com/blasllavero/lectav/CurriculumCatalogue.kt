/*
 * LECTAV — starter subject catalogue for Catalunya, 2026–2027
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Official titles in Catalan. This is an attributed, intentionally PARTIAL
 * selection. No generative AI is used for the list. More subjects require review.
 */
package com.blasllavero.lectav

import androidx.annotation.StringRes

internal enum class SubjectKind(@StringRes val labelRes: Int) {
    COMMON(R.string.catalogue_kind_common),
    SCHOOL_SCHEDULE(R.string.catalogue_kind_variable),
    OPTIONAL(R.string.catalogue_kind_optional),
    MODALITY(R.string.catalogue_kind_modality)
}

internal data class SubjectOption(
    val id: String,
    val officialNameCa: String,
    val kind: SubjectKind,
    val sourceUrl: String
)

private const val ESO_SOURCE = "https://projectes.xtec.cat/nou-curriculum/educacio-basica/decret-educacio-basica/"
private const val BATX_SOURCE = "https://projectes.xtec.cat/nou-curriculum/batxillerat/decret-batxillerat/"

private fun eso(id: String, name: String, kind: SubjectKind = SubjectKind.COMMON) =
    SubjectOption(id, name, kind, ESO_SOURCE)

private fun batx(id: String, name: String, kind: SubjectKind = SubjectKind.COMMON) =
    SubjectOption(id, name, kind, BATX_SOURCE)

// Decret 175/2022 as amended by Decret 480/2024 (Annex 7).
// Contents of the following subjects are present in every course from 1st to 3rd ESO.
private val eso1to3Core = listOf(
    eso("geografia-historia", "Ciències Socials: Geografia i Història"),
    eso("educacio-fisica", "Educació Física"),
    eso("llengua-castellana", "Llengua Castellana i Literatura"),
    eso("llengua-catalana", "Llengua Catalana i Literatura"),
    eso("llengua-estrangera", "Llengua Estrangera"),
    eso("matematiques", "Matemàtiques")
)

// The school assigns these subjects across the first three courses.
// In particular the science, arts/music and technology offerings vary by year.
private val eso1to3SchoolSchedule = listOf(
    eso("biologia-geologia", "Biologia i Geologia", SubjectKind.SCHOOL_SCHEDULE),
    eso("fisica-quimica", "Física i Química", SubjectKind.SCHOOL_SCHEDULE),
    eso("educacio-plastica", "Educació Plàstica, Visual i Audiovisual", SubjectKind.SCHOOL_SCHEDULE),
    eso("musica", "Música", SubjectKind.SCHOOL_SCHEDULE),
    eso("tecnologia-digitalitzacio", "Tecnologia i Digitalització", SubjectKind.SCHOOL_SCHEDULE)
)

// Optional course offerings depend on the school. Only a reviewed starter set.
private val eso1to3Optional = listOf(
    eso("segona-llengua", "Segona Llengua Estrangera", SubjectKind.OPTIONAL),
    eso("cultura-classica", "Cultura Clàssica", SubjectKind.OPTIONAL),
    eso("emprenedoria", "Emprenedoria", SubjectKind.OPTIONAL),
    eso("robotica-programacio", "Robòtica i Programació", SubjectKind.OPTIONAL)
)

private val eso4 = listOf(
    eso("geografia-historia", "Ciències Socials: Geografia i Història"),
    eso("valors-civics", "Educació en Valors Cívics i Ètics"),
    eso("educacio-fisica", "Educació Física"),
    eso("llengua-castellana", "Llengua Castellana i Literatura"),
    eso("llengua-catalana", "Llengua Catalana i Literatura"),
    eso("llengua-estrangera", "Llengua Estrangera"),
    eso("matematiques", "Matemàtiques"),
    eso("cultura-cientifica", "Cultura Científica", SubjectKind.OPTIONAL)
)

// Batxillerat: Decret 171/2022 as amended by Decret 103/2026.
// 2026–27 changes take effect for year 1; some year-2 changes in 2027–28.
// These lists intentionally include only verified common subjects and selected
// modality subjects. They are NOT a complete institution-specific timetable.
private val batx1 = listOf(
    batx("llengua-catalana", "Llengua Catalana i Literatura I"),
    batx("llengua-castellana", "Llengua Castellana i Literatura I"),
    batx("llengua-estrangera", "Llengua Estrangera I"),
    batx("educacio-fisica", "Educació Física"),
    batx("filosofia", "Filosofia"),
    batx("matematiques", "Matemàtiques I", SubjectKind.MODALITY),
    batx("matematiques-generals", "Matemàtiques Generals", SubjectKind.MODALITY),
    batx("matematiques-socials", "Matemàtiques Aplicades a les Ciències Socials I", SubjectKind.MODALITY),
    batx("llati", "Llatí I", SubjectKind.MODALITY),
    batx("biologia-geologia", "Biologia, Geologia i Ciències Ambientals", SubjectKind.MODALITY),
    batx("fisica-quimica", "Física i Química", SubjectKind.MODALITY),
    batx("dibuix-tecnic", "Dibuix Tècnic I", SubjectKind.MODALITY),
    batx("tecnologia-enginyeria", "Tecnologia i Enginyeria I", SubjectKind.MODALITY)
)

private val batx2 = listOf(
    batx("llengua-catalana", "Llengua Catalana i Literatura II"),
    batx("llengua-castellana", "Llengua Castellana i Literatura II"),
    batx("llengua-estrangera", "Llengua Estrangera II"),
    batx("historia", "Història"),
    batx("historia-filosofia", "Història de la Filosofia"),
    batx("matematiques", "Matemàtiques II", SubjectKind.MODALITY),
    batx("matematiques-socials", "Matemàtiques Aplicades a les Ciències Socials II", SubjectKind.MODALITY),
    batx("biologia", "Biologia", SubjectKind.MODALITY),
    batx("fisica", "Física", SubjectKind.MODALITY),
    batx("quimica", "Química", SubjectKind.MODALITY),
    batx("geologia-ambientals", "Geologia i Ciències Ambientals", SubjectKind.MODALITY),
    batx("dibuix-tecnic", "Dibuix Tècnic II", SubjectKind.MODALITY),
    batx("tecnologia-enginyeria", "Tecnologia i Enginyeria II", SubjectKind.MODALITY)
)

internal fun catalogueSubjectsFor(course: String): List<SubjectOption> = when (course) {
    "eso1", "eso2", "eso3" -> eso1to3Core + eso1to3SchoolSchedule + eso1to3Optional
    "eso4" -> eso4
    "batx1" -> batx1
    "batx2" -> batx2
    else -> emptyList()
}
