/*
 * LECTAV — original text companion for PDF materials generated on-device.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PDF created by WebView.draw() can be visually correct but have no
 * extractable text. Preserve the teacher-reviewed original Markdown in a
 * separate PRIVATE UTF-8 sidecar. It never changes the PDF appearance.
 *
 * No raw text is uploaded, and no arbitrary content:// provider path is used
 * to access local disk. Sidecar files are not exposed through FileProvider.
 */
internal object LocalPdfSource {
    const val MAX_CHARS = 50_000
    private val allowedGeneratedPdf = Regex("""(?:lectav|backup)_[A-Za-z0-9_-]{3,120}\.pdf""")

    private fun sidecarForUri(context: Context, uri: Uri): File? {
        if (uri.scheme != "content" ||
            uri.authority != "${context.packageName}.files") return null
        val filename = uri.lastPathSegment ?: return null
        if (!allowedGeneratedPdf.matches(filename)) return null
        val dir = File(context.filesDir, "generated")
        if (!File(dir, filename).isFile) return null
        return File(dir, filename.removeSuffix(".pdf") + ".source.txt")
    }

    fun sidecarForGeneratedFile(pdf: File): File {
        require(allowedGeneratedPdf.matches(pdf.name)) {
            "El PDF no pertany al generador local."
        }
        require(pdf.isFile) { "No s'ha desat el PDF." }
        return File(pdf.parentFile, pdf.name.removeSuffix(".pdf") + ".source.txt")
    }

    private fun atomicWrite(destination: File, source: String) {
        require(source.length in 1..MAX_CHARS)
        val staging = File(destination.parentFile, destination.name + ".partial")
        try {
            staging.writeText(source, Charsets.UTF_8)
            check(staging.renameTo(destination)) {
                "No s'ha pogut conservar el text original del document."
            }
        } finally {
            staging.delete()
        }
    }

    private val materialIdPattern = Regex("""[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}""")

    private fun forMaterialId(context: Context, id: String): File {
        require(materialIdPattern.matches(id)) { "Identificador de material incorrecte." }
        val folder = File(context.filesDir, "speech_sources")
        check(folder.isDirectory || folder.mkdirs()) { "No es pot crear la carpeta de guions." }
        return File(folder, "$id.txt")
    }

    /** Associate an approved transcript to an older/generated/imported PDF. */
    suspend fun saveForMaterial(context: Context, material: LocalMaterial, text: String) =
        withContext(Dispatchers.IO) {
            require(material.type == "lesson" || material.type == "transcript")
            atomicWrite(forMaterialId(context, material.id), text)
        }

    suspend fun saveForMaterialId(context: Context, id: String, text: String) =
        withContext(Dispatchers.IO) {
            atomicWrite(forMaterialId(context, id), text)
        }

    suspend fun readForMaterial(context: Context, material: LocalMaterial): String? =
        withContext(Dispatchers.IO) {
            if(material.type != "lesson" && material.type != "transcript")
                return@withContext null
            val file = runCatching { forMaterialId(context,material.id) }.getOrNull()
            if(file != null && file.isFile && file.length() in 1..(MAX_CHARS * 4L)) {
                return@withContext file.readText(Charsets.UTF_8).takeIf { it.length in 1..MAX_CHARS }
            }
            read(context, material.uri)
        }

    suspend fun saveForGeneratedPdf(pdf: File, text: String) = withContext(Dispatchers.IO) {
        atomicWrite(sidecarForGeneratedFile(pdf), text)
    }

    suspend fun saveForUri(context: Context, uri: String, text: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = sidecarForUri(context, Uri.parse(uri)) ?: return@withContext false
            atomicWrite(file, text)
            true
        }

    suspend fun read(context: Context, uri: String): String? = withContext(Dispatchers.IO) {
        val file = sidecarForUri(context, Uri.parse(uri)) ?: return@withContext null
        if (!file.isFile || file.length() !in 1..(MAX_CHARS * 4L)) return@withContext null
        file.readText(Charsets.UTF_8).takeIf { it.length in 1..MAX_CHARS }
    }
}
