/*
 * LECTAV — encrypted portable backups: settings and locally accessible materials.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal data class BackupResult(val files: Int, val courses: Int, val bytes: Long)
internal data class RestoredBackup(
    val files: Int, val courses: Int, val languageTag: String, val accountHint: String
)

/** 8-byte magic + 16-byte salt + 12-byte IV + AES-256-GCM encrypted ZIP.
 *  PBKDF2-HMAC-SHA256 with 220k rounds. No OAuth credentials are exported.
 */
internal object LectavBackup {
    private val magic = "LECTAVB1".toByteArray(Charsets.US_ASCII)
    private val random = SecureRandom()
    private const val MAX_SIZE = 250L * 1024 * 1024
    private const val MAX_ITEMS = 500
    private const val MAX_MANIFEST = 1_200_000

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        require(password.length in 10..200) {
            "La contrasenya ha de tenir entre 10 i 200 caràcters."
        }
        val letters = password.toCharArray()
        val spec = PBEKeySpec(letters, salt, 220_000, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") }
            finally { bytes.fill(0) }
        } finally {
            spec.clearPassword()
            letters.fill('\u0000')
        }
    }

    private fun settingsToJson(settings: SavedSettings): JSONObject {
        val courses = JSONArray()
        settings.savedCourses.forEach { c ->
            courses.put(JSONObject()
                .put("id",c.id).put("role",c.role).put("pathway",c.pathway)
                .put("course",c.course).put("subject",c.subject)
                .put("at",c.lastOpenedAt))
        }
        return JSONObject()
            .put("country",settings.countryCode).put("region",settings.regionCode)
            .put("pathway",settings.pathway).put("course",settings.course)
            .put("subject",settings.subject)
            .put("subtitle",settings.subtitleHeight.toDouble())
            .put("role",settings.activeRole).put("courses",courses)
    }

    private fun settingsFromJson(json: JSONObject): SavedSettings {
        val courses = json.getJSONArray("courses")
        require(courses.length() <= MAX_ITEMS)
        val records = (0 until courses.length()).map { index ->
            val obj = courses.getJSONObject(index)
            val role = obj.getString("role")
            val pathway = obj.getString("pathway")
            val year = obj.getString("course")
            val subject = obj.getString("subject")
            require(role in listOf("teacher","student") &&
                pathway in listOf("eso","batx") &&
                year.length in 1..40 && subject.length in 1..100)
            SavedCourse(
                id=obj.getString("id").take(250),
                role=role, pathway=pathway,course=year, subject=subject,
                lastOpenedAt=obj.optLong("at")
            )
        }.distinctBy { it.id }
        return SavedSettings(
            countryCode=json.optString("country","ES").take(12),
            regionCode=json.optString("region","ES-CT").take(24),
            pathway=json.optString("pathway").take(40),
            course=json.optString("course").take(40),
            subject=json.optString("subject").take(100),
            subtitleHeight=json.optDouble("subtitle",0.25).toFloat().coerceIn(0f,1f),
            activeRole=if(json.optString("role")=="student") "student" else "teacher",
            savedCourses=records
        )
    }

    private fun curricularSources(context: Context): JSONObject {
        val result = JSONObject()
        context.getSharedPreferences(
            "lectav_official_curriculum",Context.MODE_PRIVATE
        ).all.forEach { (name,value) ->
            if (name.length <= 200 && value is String && value.length <= 100_000)
                result.put(name,value)
        }
        return result
    }

    private fun restoreSources(context: Context, value: JSONObject) {
        require(value.length() <= MAX_ITEMS)
        val changes = context.getSharedPreferences(
            "lectav_official_curriculum",Context.MODE_PRIVATE
        ).edit().clear()
        value.keys().forEach { name ->
            val v = value.getString(name)
            require(name.length <= 200 && v.length <= 100_000)
            changes.putString(name,v)
        }
        check(changes.commit()) { "No s'ha desat la informació curricular." }
    }

    private fun generatorPrefs(context: Context): JSONObject {
        val prefs=context.getSharedPreferences("lectav_generator",Context.MODE_PRIVATE)
        val names=listOf("language","kind","mode","endpoint","ollamaModel","chatgptModel")
        val result=JSONObject()
        names.forEach { name ->
            result.put(name,prefs.getString(name,"").orEmpty().take(120))
        }
        return result
    }

    private fun restoreGeneratorPrefs(context: Context, values: JSONObject) {
        val prefs=context.getSharedPreferences("lectav_generator",Context.MODE_PRIVATE)
        val edit=prefs.edit().clear()
        listOf("language","kind","mode","endpoint","ollamaModel","chatgptModel")
            .forEach { name -> edit.putString(name,values.optString(name).take(120)) }
        check(edit.commit()) { "No s'han restaurat les preferències del generador." }
    }

    suspend fun export(
        context: Context, destination: Uri, password: String,
        settingsRepo: SettingsRepository, plansRepo: CoursePlanningRepository,
        library: MaterialLibrary, usage: ChatGPTUsageGuard, language: String
    ): BackupResult = withContext(Dispatchers.IO) {
        val settings = settingsRepo.snapshot()
        val documents = library.files.first()
        require(documents.size <= MAX_ITEMS)
        val infos = JSONArray()
        documents.forEachIndexed { i, file ->
            require(file.type in setOf("lesson","exercise","solutions","video","audio","podcast","transcript"))
            val extension = when(file.type) {
                "video" -> ".mp4"
                "audio", "podcast" -> if(file.name.endsWith(".wav",true)) ".wav" else ".mp3"
                else -> ".pdf"
            }
            infos.put(JSONObject()
                .put("id",file.id).put("courseKey",file.courseKey)
                .put("name",file.name).put("type",file.type)
                .put("opened",file.opened).put("completed",file.completed)
                .put("origin",file.origin).put("sessionKey",file.sessionKey)
                .put("entry","files/" + i.toString().padStart(4,'0') + extension))
        }
        val metadata = JSONObject().put("version",1)
            .put("settings",settingsToJson(settings))
            .put("plans",plansRepo.snapshotRaw())
            .put("locale",language.take(80))
            .put("generator",generatorPrefs(context))
            .put("curriculum",curricularSources(context))
            .put("chatgptHint",ChatGPTAuth.state.value.email.take(254))
            .put("materials",infos)
        val manifest = metadata.toString().toByteArray(Charsets.UTF_8)
        require(manifest.size in 1..MAX_MANIFEST) {
            "La configuració supera la mida admesa."
        }

        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
        }
        var total = 0L
        val stream = context.contentResolver.openOutputStream(destination,"wt")
            ?: throw IOException("No es pot obrir el fitxer de còpia.")
        stream.use { raw ->
            raw.write(magic)
            raw.write(salt)
            raw.write(iv)
            ZipOutputStream(CipherOutputStream(raw,cipher)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest)
                zip.closeEntry()
                documents.forEachIndexed { index, file ->
                    val entry = infos.getJSONObject(index).getString("entry")
                    context.contentResolver.openInputStream(Uri.parse(file.uri)).use { input ->
                        require(input != null) { "No es pot llegir: " + file.name }
                        zip.putNextEntry(ZipEntry(entry))
                        val buf=ByteArray(64*1024)
                        var size=0L
                        while(true) {
                            val n=input.read(buf)
                            if(n==-1) break
                            size += n
                            total += n
                            require(total <= MAX_SIZE) {
                                "Còpia massa gran (màxim 250 MB). " +
                                "Exporta els vídeos grans a la SD per separat."
                            }
                            zip.write(buf,0,n)
                        }
                        require(size > 0) { "Document buit: " + file.name }
                        zip.closeEntry()
                    }
                }
            }
        }
        BackupResult(documents.size,settings.savedCourses.size,total)
    }

    suspend fun restore(
        context: Context, source: Uri, password: String,
        settingsRepo: SettingsRepository, plansRepo: CoursePlanningRepository,
        library: MaterialLibrary, usage: ChatGPTUsageGuard
    ): RestoredBackup = withContext(Dispatchers.IO) {
        require(!ChatGPTAuth.state.value.connected) {
            "Desconnecta ChatGPT abans de restaurar un altre perfil."
        }
        val folder = File(context.cacheDir,"lectav_import")
            .apply { if(!isDirectory) check(mkdirs()) }
        val stagedArchive = File.createTempFile("lectav_backup_",".zip",folder)
        val newFiles = ArrayList<File>()
        var changesStarted=false
        try {
            // Decrypt entire stream and check AES-GCM authentication *before*
            // modifying any real user preferences or library index.
            context.contentResolver.openInputStream(source).use { input ->
                require(input != null) { "No es pot obrir la còpia." }
                val stream=DataInputStream(input)
                val found=ByteArray(8).also(stream::readFully)
                require(found.contentEquals(magic)) {
                    "Aquest fitxer no és una còpia xifrada de LECTAV."
                }
                val salt=ByteArray(16).also(stream::readFully)
                val iv=ByteArray(12).also(stream::readFully)
                val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
                }
                CipherInputStream(stream,cipher).use { plaintext ->
                    stagedArchive.outputStream().use { out ->
                        val buf=ByteArray(64*1024)
                        var bytes=0L
                        while(true) {
                            val n=plaintext.read(buf)
                            if(n==-1) break
                            bytes += n
                            require(bytes <= MAX_SIZE + 12L*1024*1024) {
                                "Còpia massa gran o danyada."
                            }
                            out.write(buf,0,n)
                        }
                    }
                }
            }

            ZipFile(stagedArchive).use { zip ->
                val manifestEntry=zip.getEntry("manifest.json")
                    ?: error("La còpia no té un índex de configuració.")
                require(manifestEntry.size in 1..MAX_MANIFEST.toLong())
                val rawManifest=zip.getInputStream(manifestEntry).use { bytes ->
                    bytes.readBytes().also { require(it.size <= MAX_MANIFEST) }
                }
                val manifest=JSONObject(rawManifest.toString(Charsets.UTF_8))
                require(manifest.getInt("version")==1) { "Format incompatible." }
                val restoredSettings=settingsFromJson(manifest.getJSONObject("settings"))
                val plans=manifest.getString("plans")
                require(plans.length <= 350_000)
                val curriculum=manifest.getJSONObject("curriculum")
                val items=manifest.getJSONArray("materials")
                require(items.length() <= MAX_ITEMS && zip.size()==items.length()+1) {
                    "La còpia conté fitxers inesperats."
                }
                val usedNames=HashSet<String>()
                val restored=ArrayList<LocalMaterial>()
                val privateDir=File(context.filesDir,"generated")
                    .apply { if(!isDirectory) check(mkdirs()) }
                var total=0L
                for(i in 0 until items.length()) {
                    val obj=items.getJSONObject(i)
                    val type=obj.getString("type")
                    require(type in setOf("lesson","exercise","solutions","video","audio","podcast","transcript"))
                    val fileName=obj.getString("entry")
                    val suffix=when(type) {
                        "video" -> ".mp4"
                        "audio", "podcast" -> if(obj.getString("name").endsWith(".wav",true)) ".wav" else ".mp3"
                        else -> ".pdf"
                    }
                    require(fileName.matches(Regex("""files/\d{4}\.(pdf|mp4|mp3|wav)""")) &&
                        fileName.endsWith(suffix) && usedNames.add(fileName))
                    val zipEntry=zip.getEntry(fileName)
                        ?: error("Falta el document " + fileName)
                    require(!zipEntry.isDirectory && zipEntry.size in 1..MAX_SIZE)
                    val local=File.createTempFile("backup_",suffix,privateDir)
                    newFiles.add(local)
                    zip.getInputStream(zipEntry).use { input ->
                        local.outputStream().use { output ->
                            val buffer=ByteArray(64*1024)
                            var copied=0L
                            while(true) {
                                val n=input.read(buffer)
                                if(n==-1) break
                                total += n
                                copied += n
                                require(total <= MAX_SIZE) { "Còpia massa gran." }
                                output.write(buffer,0,n)
                            }
                            require(copied > 0)
                        }
                    }
                    if(suffix==".pdf") {
                        val header=ByteArray(4)
                        local.inputStream().use { check(it.read(header)==4) }
                        require(header.toString(Charsets.US_ASCII)=="%PDF") {
                            "S'ha trobat un PDF invàlid."
                        }
                    }
                    val link=FileProvider.getUriForFile(
                        context,context.packageName+".files",local
                    )
                    restored.add(LocalMaterial(
                        id=obj.getString("id").take(200),
                        courseKey=obj.getString("courseKey").take(200),
                        uri=link.toString(),
                        name=obj.getString("name").take(160),
                        type=type,
                        opened=obj.optBoolean("opened"),
                        completed=obj.optBoolean("completed"),
                        origin=if(obj.optString("origin")=="generated")
                            "generated" else "restored",
                        lastExportUri="", // previous content:// grants can no longer be valid
                        sessionKey=obj.optString("sessionKey").take(150)
                    ))
                }
                // The archive and every file are authenticated/validated now.
                // If a later preference write fails, keep staged files so
                // previously written library URIs cannot point to deleted files.
                changesStarted=true
                plansRepo.restoreBackup(plans)
                settingsRepo.restoreBackup(restoredSettings)
                library.restoreBackup(restored)
                restoreSources(context,curriculum)
                restoreGeneratorPrefs(context,manifest.optJSONObject("generator") ?: JSONObject())
                // Backups from <= 0.9.0 may contain the deprecated local
                // usage counter. Ignore it; never restore a local block.
                // Actual account quotas are controlled exclusively by OpenAI.
                val hint=manifest.optString("chatgptHint").take(254)
                context.getSharedPreferences(
                    "lectav_restored_chatgpt",Context.MODE_PRIVATE
                ).edit().putString("accountHint",hint).apply()
                RestoredBackup(
                    files=restored.size,
                    courses=restoredSettings.savedCourses.size,
                    languageTag=manifest.optString("locale").take(80),
                    accountHint=hint
                )
            }
        } catch(e: Exception) {
            if(!changesStarted) newFiles.forEach { runCatching { it.delete() } }
            // Authentication errors from some providers are wrapped in IOException.
            if(e is javax.crypto.AEADBadTagException ||
                e.cause is javax.crypto.AEADBadTagException)
                throw IOException("Contrasenya incorrecta o còpia malmesa.",e)
            throw e
        } finally {
            stagedArchive.delete()
        }
    }
}
