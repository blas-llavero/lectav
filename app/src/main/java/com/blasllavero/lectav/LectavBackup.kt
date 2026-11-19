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
import java.security.MessageDigest
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

internal data class BackupSelection(
    val courseKeys: Set<String>,
    val materialIds: Set<String>
)
internal data class RestoredBackup(
    val files: Int, val courses: Int, val languageTag: String, val accountHint: String
)

/** 8-byte magic + 16-byte salt + 12-byte IV + AES-256-GCM encrypted ZIP.
 *  PBKDF2-HMAC-SHA256 with 220k rounds. No OAuth credentials are exported.
 */
internal object LectavBackup {
    private val magic = "LECTAVB1".toByteArray(Charsets.US_ASCII)
    private val random = SecureRandom()
    private const val MAX_SIZE = 1024L * 1024 * 1024
    private const val MAX_ITEMS = 500
    private const val MAX_MANIFEST = 1_200_000
    private fun sha256() = MessageDigest.getInstance("SHA-256")
    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }

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
        val names=AutoGenerationConfig.backupKeys
        val result=JSONObject()
        names.forEach { name ->
            result.put(name,prefs.getString(name,"").orEmpty().take(
                if(name=="autoOverrides") 400_000 else 400))
        }
        return result
    }

    private fun restoreGeneratorPrefs(context: Context, values: JSONObject) {
        val prefs=context.getSharedPreferences("lectav_generator",Context.MODE_PRIVATE)
        val edit=prefs.edit().clear()
        AutoGenerationConfig.backupKeys
            .forEach { name -> edit.putString(name,values.optString(name).take(
                if(name=="autoOverrides") 400_000 else 400)) }
        edit.putString("autoAuthorized","false")
        check(edit.commit()) { "No s'han restaurat les preferències del generador." }
    }

    /**
     * Read-only archive inspection. Decrypts the complete file to ensure
     * authenticated AES-GCM completion, then checks that every indexed
     * PDF/audio/video archive member is present and non-empty. It never
     * writes course preferences or changes the existing material library.
     *
     * Users can inspect historical backups before performing a replacement.
     */
    suspend fun inspect(context: Context, source: Uri, password: String): BackupResult =
        withContext(Dispatchers.IO) {
            val staging=File.createTempFile("lectav_audit_",".zip",context.cacheDir)
            try {
                context.contentResolver.openInputStream(source).use { input ->
                    require(input!=null) { "The backup file cannot be opened for inspection." }
                    val header=DataInputStream(input)
                    val found=ByteArray(8).also(header::readFully)
                    require(found.contentEquals(magic)) {
                        "This is not a LECTAV encrypted backup (.lectavbak)."
                    }
                    val salt=ByteArray(16).also(header::readFully)
                    val iv=ByteArray(12).also(header::readFully)
                    val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {
                        init(Cipher.DECRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
                    }
                    CipherInputStream(header,cipher).use { plain ->
                        staging.outputStream().buffered().use { target ->
                            val bytes=ByteArray(65536)
                            var count=0L
                            while(true) {
                                val read=plain.read(bytes)
                                if(read==-1)break
                                count+=read
                                require(count<=MAX_SIZE+12L*1024*1024) {
                                    "The encrypted backup exceeds the supported size."
                                }
                                target.write(bytes,0,read)
                            }
                        }
                    }
                }
                ZipFile(staging).use { zip ->
                    val meta=zip.getEntry("manifest.json")
                        ?: error("The backup is missing manifest.json.")
                    require(meta.size in 1..MAX_MANIFEST.toLong())
                    val manifest=JSONObject(zip.getInputStream(meta).use { stream ->
                        stream.readBytes().also { require(it.size<=MAX_MANIFEST) }
                    }.toString(Charsets.UTF_8))
                    require(manifest.getInt("version")==1) {
                        "This LECTAV backup version is not supported."
                    }
                    val courseRecords=manifest.getJSONObject("settings")
                        .getJSONArray("courses")
                    require(courseRecords.length()<=MAX_ITEMS)
                    val materials=manifest.getJSONArray("materials")
                    require(materials.length()<=MAX_ITEMS)
                    val expectedKeys=settingsFromJson(manifest.getJSONObject("settings"))
                        .savedCourses.map(::courseKey).toSet()
                    val filenames=HashSet<String>()
                    var sourceCount=0
                    var checksumCount=0
                    var bytesTotal=0L
                    for(i in 0 until materials.length()) {
                        val item=materials.getJSONObject(i)
                        val type=item.getString("type")
                        require(type in setOf("lesson","exercise","solutions",
                            "video","audio","podcast","transcript"))
                        require(item.getString("courseKey") in expectedKeys) {
                            "A material is not attached to a course in this backup."
                        }
                        val file=item.getString("entry")
                        require(file.matches(Regex("""files/\d{4}\.(pdf|mp3|wav|mp4)""")) &&
                            filenames.add(file))
                        val entry=zip.getEntry(file) ?: error("Missing archived material: $file")
                        require(!entry.isDirectory && entry.size in 1..MAX_SIZE)
                        val checkName="checksums/" +
                            i.toString().padStart(4,'0')+".json"
                        val check=zip.getEntry(checkName)?.let {
                            checksumCount++
                            JSONObject(zip.getInputStream(it).use { stream ->
                                stream.readBytes().also { require(it.size<=1000) }
                            }.toString(Charsets.UTF_8))
                        }
                        val hash=sha256()
                        var copied=0L
                        val firstBytes=ByteArray(4)
                        zip.getInputStream(entry).buffered().use { sourceBytes ->
                            val buffer=ByteArray(65536)
                            while(true) {
                                val n=sourceBytes.read(buffer)
                                if(n==-1)break
                                if(copied<4) {
                                    val take=minOf(n,(4-copied).toInt())
                                    buffer.copyInto(firstBytes,copied.toInt(),0,take)
                                }
                                hash.update(buffer,0,n)
                                copied+=n
                                bytesTotal+=n
                                require(bytesTotal<=MAX_SIZE)
                            }
                        }
                        require(copied>0)
                        if(file.endsWith(".pdf")) require(
                            copied>=4 &&
                                firstBytes.toString(Charsets.US_ASCII)=="%PDF"
                        ) { "The backup contains an invalid PDF: $file" }
                        if(check!=null) require(
                            check.getString("entry")==file &&
                                check.getLong("bytes")==copied &&
                                check.getString("sha256")==hex(hash.digest())
                        ) { "Checksum mismatch: $file" }
                        if(item.has("sourceEntry")) {
                            val sourceName=item.getString("sourceEntry")
                            require(sourceName=="sources/" +
                                i.toString().padStart(4,'0')+".txt")
                            val companion=zip.getEntry(sourceName)
                                ?: error("Missing narration source: $sourceName")
                            require(companion.size in 1..200_000)
                            sourceCount++
                        }
                    }
                    require(checksumCount==0 || checksumCount==materials.length()) {
                        "The backup contains incomplete integrity records."
                    }
                    require(zip.size()==1+materials.length()+sourceCount+
                        checksumCount) {
                        "The backup contains unexpected or missing ZIP members."
                    }
                    BackupResult(materials.length(),courseRecords.length(),bytesTotal)
                }
            } finally {
                staging.delete()
            }
        }

    suspend fun export(
        context: Context, destination: Uri, password: String,
        settingsRepo: SettingsRepository, plansRepo: CoursePlanningRepository,
        library: MaterialLibrary, usage: ChatGPTUsageGuard, language: String,
        selection: BackupSelection? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        val snapshot = settingsRepo.snapshot()
        val settings = if(selection==null) snapshot else snapshot.copy(
            savedCourses=snapshot.savedCourses.filter { courseKey(it) in selection.courseKeys }
        )
        val documents = library.files.first().filter {
            selection==null || (it.courseKey in selection.courseKeys
                && it.id in selection.materialIds)
        }
        require(documents.size <= MAX_ITEMS && settings.savedCourses.size <= MAX_ITEMS)
        if(selection!=null)require(settings.savedCourses.isNotEmpty()) {
            "Selecciona almenys una assignatura."
        }
        // Generated source text is included in the encrypted backup.
        val sources = documents.map { file ->
            if(file.type=="lesson" || file.type=="transcript")
                LocalPdfSource.readForMaterial(context,file)
            else null
        }
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
                .put("createdAt",file.createdAt)
                .put("origin",file.origin).put("sessionKey",file.sessionKey)
                .put("entry","files/" + i.toString().padStart(4,'0') + extension)
                .apply {
                    if(sources[i]!=null)
                        put("sourceEntry","sources/" + i.toString().padStart(4,'0') + ".txt")
                })
        }
        val fullPlans = plansRepo.snapshotRaw()
        val subsetPlans = if(selection==null) fullPlans else {
            val original=JSONArray(fullPlans)
            JSONArray().apply {
                for(i in 0 until original.length()) {
                    val item=original.getJSONObject(i)
                    if(item.optString("key") in selection.courseKeys)put(item)
                }
            }.toString()
        }
        val metadata = JSONObject().put("version",1)
            .put("partial",selection!=null)
            .put("settings",settingsToJson(settings))
            .put("plans",subsetPlans)
            .put("locale",if(selection==null)language.take(80) else "")
            .put("generator",if(selection==null)generatorPrefs(context) else JSONObject())
            .put("curriculum",if(selection==null)curricularSources(context) else JSONObject())
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
                        val hash=sha256()
                        val buf=ByteArray(64*1024)
                        var size=0L
                        while(true) {
                            val n=input.read(buf)
                            if(n==-1) break
                            size += n
                            total += n
                            hash.update(buf,0,n)
                            require(total <= MAX_SIZE) {
                                "Còpia massa gran (màxim 1 GB). " +
                                "Exporta els vídeos grans a la SD per separat."
                            }
                            zip.write(buf,0,n)
                        }
                        require(size > 0) { "Empty document: " + file.name }
                        zip.closeEntry()
                        val check=JSONObject().put("entry",entry)
                            .put("bytes",size).put("sha256",hex(hash.digest()))
                        zip.putNextEntry(ZipEntry("checksums/" +
                            index.toString().padStart(4,'0') + ".json"))
                        zip.write(check.toString().toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                    sources[index]?.let { sourceText ->
                        val sourceEntry=infos.getJSONObject(index).getString("sourceEntry")
                        val content=sourceText.toByteArray(Charsets.UTF_8)
                        total+=content.size
                        require(total<=MAX_SIZE) { "La còpia supera el límit d\'1 GB." }
                        zip.putNextEntry(ZipEntry(sourceEntry))
                        zip.write(content)
                        zip.closeEntry()
                    }
                }
            }
        }
        // Verify the actual destination archive, not merely the list of
        // intended source files. Do not tell the user export succeeded if
        // Android's document provider did not persist the material bytes.
        val verified=inspect(context,destination,password)
        check(verified.files==documents.size &&
            verified.courses==settings.savedCourses.size) {
            "The exported backup does not contain the expected materials."
        }
        verified
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
                val sourceEntries=(0 until items.length()).count {
                    items.getJSONObject(it).has("sourceEntry")
                }
                require(items.length() <= MAX_ITEMS)
                val checksumCount=(0 until items.length()).count { index ->
                    zip.getEntry("checksums/" +
                        index.toString().padStart(4,'0') + ".json")!=null
                }
                require(checksumCount==0 || checksumCount==items.length()) {
                    "Incomplete backup integrity records."
                }
                require(zip.size()==1+items.length()+sourceEntries+checksumCount) {
                    "Backup ZIP has unexpected or missing files."
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
                    val checkEntry=zip.getEntry("checksums/" +
                        i.toString().padStart(4,'0') + ".json")
                    val expected=checkEntry?.let { record ->
                        JSONObject(zip.getInputStream(record).use { it.readBytes() }
                            .toString(Charsets.UTF_8)).also { item ->
                            require(item.getString("entry")==fileName)
                            require(item.getLong("bytes") in 1..MAX_SIZE)
                            require(item.getString("sha256").matches(Regex("[a-f0-9]{64}")))
                        }
                    }
                    zip.getInputStream(zipEntry).use { input ->
                        local.outputStream().use { output ->
                            val hash=sha256()
                            val buffer=ByteArray(64*1024)
                            var copied=0L
                            while(true) {
                                val n=input.read(buffer)
                                if(n==-1) break
                                total += n
                                copied += n
                                hash.update(buffer,0,n)
                                require(total <= MAX_SIZE) { "Còpia massa gran." }
                                output.write(buffer,0,n)
                            }
                            require(copied > 0)
                            if(expected!=null) require(
                                expected.getLong("bytes")==copied &&
                                expected.getString("sha256")==hex(hash.digest())
                            ) { "Backup integrity check failed: " + fileName }
                        }
                    }
                    if(suffix==".pdf") {
                        val header=ByteArray(4)
                        local.inputStream().use { check(it.read(header)==4) }
                        require(header.toString(Charsets.US_ASCII)=="%PDF") {
                            "S'ha trobat un PDF invàlid."
                        }
                    }
                    if(obj.has("sourceEntry")) {
                        val sourceEntry=obj.getString("sourceEntry")
                        require(suffix==".pdf" &&
                            sourceEntry=="sources/" + i.toString().padStart(4,'0') + ".txt") {
                            "Índex de text original invàlid."
                        }
                        val entry=zip.getEntry(sourceEntry)
                            ?: error("Falta el text original de " + fileName)
                        require(!entry.isDirectory && entry.size in 1..200_000) {
                            "Text original invàlid o excessiu."
                        }
                        val bytes=zip.getInputStream(entry).use { stream ->
                            stream.readBytes().also { require(it.size in 1..200_000) }
                        }
                        val original=bytes.toString(Charsets.UTF_8)
                        require(original.length in 1..LocalPdfSource.MAX_CHARS)
                        LocalPdfSource.saveForGeneratedPdf(local,original)
                        newFiles.add(LocalPdfSource.sidecarForGeneratedFile(local))
                        // Restore separately linked scripts for older PDFs as well.
                        val id=obj.getString("id")
                        if(id.matches(Regex("""[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"""))) {
                            LocalPdfSource.saveForMaterialId(context,id,original)
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
                        sessionKey=obj.optString("sessionKey").take(150),
                        createdAt=obj.optLong("createdAt",0L).coerceAtLeast(0L)
                    ))
                }
                // Verify files, index links and course membership before
                // modifying any existing library or saved course.
                val exportedKeys=restoredSettings.savedCourses.map(::courseKey).toSet()
                require(restored.all { it.courseKey in exportedKeys }) {
                    "Materials reference missing courses in this archive."
                }
                restored.forEach { material ->
                    context.contentResolver.openInputStream(Uri.parse(material.uri)).use {
                        require(it!=null && it.read()>=0) {
                            "Restored material is not readable: " + material.name
                        }
                    }
                }
                if(!manifest.optBoolean("partial",false) && restored.isEmpty()) {
                    require(library.files.first().isEmpty()) {
                        "This full backup contains NO materials. Restoring it " +
                            "would erase local documents. Export a full backup " +
                            "containing all materials from the original device."
                    }
                }
                // The archive and every file are authenticated/validated now.
                // If a later preference write fails, keep staged files so
                // previously written library URIs cannot point to deleted files.
                changesStarted=true
                if(manifest.optBoolean("partial",false)){
                    // Selective imports merge, retaining unrelated local courses.
                    plansRepo.mergeBackup(plans)
                    settingsRepo.mergeBackupCourses(restoredSettings.savedCourses)
                    library.mergeBackup(restored)
                }else{
                    plansRepo.restoreBackup(plans)
                    settingsRepo.restoreBackup(restoredSettings)
                    library.restoreBackup(restored)
                    restoreSources(context,curriculum)
                    restoreGeneratorPrefs(context,manifest.optJSONObject("generator") ?: JSONObject())
                }
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
