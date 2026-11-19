/*
 * LECTAV — verified automatic export to Android document tree folders.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** SAF grants stay on the device: never put tree URIs in portable backups. */
internal object AutomaticExportFolder {
    private const val PREFS="lectav_automatic_export"
    private const val TREE="treeUri"
    private fun prefs(ctx: Context)=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)

    fun selectedTree(ctx: Context): Uri? {
        val raw=prefs(ctx).getString(TREE,"").orEmpty()
        if(raw.isBlank())return null
        val tree=Uri.parse(raw)
        return if(ctx.contentResolver.persistedUriPermissions.any {
            it.uri==tree && it.isReadPermission && it.isWritePermission
        }) tree else null
    }

    fun saveTree(ctx: Context,tree: Uri) {
        require(tree.scheme=="content") { "Choose a folder using the Android picker." }
        ctx.contentResolver.takePersistableUriPermission(tree,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        check(prefs(ctx).edit().putString(TREE,tree.toString()).commit())
    }

    fun clear(ctx:Context) {
        check(prefs(ctx).edit().remove(TREE).commit())
    }

    private fun safeSegment(input:String):String=input.trim()
        .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""),"_")
        .replace(Regex("""\s+"""),"_")
        .trim('.', '_',' ').take(72).ifBlank{"Unspecified"}

    fun relativePath(course:SavedCourse,unit:TopicPlan,index:Int,n:Int):List<String> {
        require(index>=0 && n in 1..unit.sessions)
        val pathway=when(course.pathway) {
            "eso"->"ESO";"batx"->"Batxillerat"
            else->safeSegment(course.pathway)
        }
        return listOf(pathway,safeSegment(course.course),safeSegment(course.subject),
            "Trimester_"+unit.trimester,
            "Unit_"+(index+1).toString().padStart(2,'0')+"_"+
                safeSegment(unit.id).take(28),
            "Session_"+n.toString().padStart(2,'0'))
    }

    private fun ensureFolder(parent:DocumentFile,name:String):DocumentFile {
        val found=parent.findFile(name)
        if(found!=null) {
            require(found.isDirectory) { "An export file conflicts with folder: "+name }
            return found
        }
        return parent.createDirectory(name) ?:
            throw IOException("Could not create the export folder: "+name)
    }

    fun wasExported(ctx:Context,item:LocalMaterial):Boolean {
        val tree=selectedTree(ctx)?:return false
        val last=item.lastExportUri.takeIf{it.startsWith("content://")}?:return false
        if(prefs(ctx).getString("saved_"+item.id,"")!=tree.toString()+"|"+last)
            return false
        return runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(last)).use {
                it!=null && it.read()>=0
            }
        }.getOrDefault(false)
    }

    private suspend fun hash(ctx:Context,uri:Uri):Pair<Long,ByteArray> =
        withContext(Dispatchers.IO) {
            val digest=MessageDigest.getInstance("SHA-256")
            var length=0L
            val input=ctx.contentResolver.openInputStream(uri)
                ?:throw IOException("A material is no longer readable.")
            input.use {
                val bytes=ByteArray(65536)
                while(true) {
                    coroutineContext.ensureActive()
                    val n=it.read(bytes)
                    if(n<0)break
                    digest.update(bytes,0,n)
                    length+=n
                }
            }
            require(length>0) { "Cannot export an empty material." }
            length to digest.digest()
        }

    /**
     * Validates destination bytes; never overwrites an earlier different
     * document and never deletes a source. Retries reuse identical files.
     */
    suspend fun export(ctx:Context,library:MaterialLibrary,material:LocalMaterial,
        course:SavedCourse,unit:TopicPlan,unitIndex:Int,session:Int):Uri =
        withContext(Dispatchers.IO) {
            val tree=selectedTree(ctx) ?:
                error("Choose an automatic export folder in Settings first.")
            var dir=DocumentFile.fromTreeUri(ctx,tree)
                ?: error("Cannot open the selected export folder.")
            require(dir.isDirectory && dir.canWrite()) {
                "The export folder cannot be written to; select it again."
            }
            for(name in relativePath(course,unit,unitIndex,session)) {
                coroutineContext.ensureActive()
                dir=ensureFolder(dir,name)
            }
            val source=Uri.parse(material.uri)
            val sourceHash=hash(ctx,source)
            val raw=material.name.trim()
            val ext=raw.substringAfterLast('.', "").lowercase()
            require(ext in setOf("pdf","wav","mp3","mp4")) {
                "This material has an unsupported file extension."
            }
            val base=safeSegment(raw.substringBeforeLast('.',raw))
            val mime=materialMime(material.type,raw)
            var name=base+"."+ext
            var revision=2
            var existing:DocumentFile?=null
            while(true) {
                coroutineContext.ensureActive()
                val candidate=dir.findFile(name) ?:break
                require(candidate.isFile) { "An export name conflicts with a folder." }
                val same=runCatching {
                    val check=hash(ctx,candidate.uri)
                    check.first==sourceHash.first &&
                        check.second.contentEquals(sourceHash.second)
                }.getOrDefault(false)
                if(same) {existing=candidate;break}
                require(revision<=99) { "Too many files with the same name." }
                name=base.take(90)+"_revision_"+revision+"."+ext
                revision++
            }
            val target=existing ?: dir.createFile(mime,name)
                ?:throw IOException("Cannot create a file in the export folder.")
            if(existing==null) {
                var finished=false
                try {
                    val input=ctx.contentResolver.openInputStream(source)
                        ?:throw IOException("The source file cannot be opened.")
                    input.use { original ->
                        val output=ctx.contentResolver.openOutputStream(target.uri,"wt")
                            ?:throw IOException("The export destination cannot be opened.")
                        output.use { dest ->
                            val bytes=ByteArray(65536)
                            var count=0L
                            while(true) {
                                coroutineContext.ensureActive()
                                val n=original.read(bytes)
                                if(n<0)break
                                dest.write(bytes,0,n)
                                count+=n
                            }
                            require(count==sourceHash.first) {
                                "Source length changed while exporting."
                            }
                            dest.flush()
                        }
                    }
                    val check=hash(ctx,target.uri)
                    require(check.first==sourceHash.first &&
                        check.second.contentEquals(sourceHash.second)) {
                        "The exported file failed SHA-256 verification."
                    }
                    finished=true
                } finally {
                    if(!finished)runCatching { target.delete() }
                }
            }
            library.recordExport(material.id,target.uri.toString())
            check(prefs(ctx).edit().putString("saved_"+material.id,
                tree.toString()+"|"+target.uri).commit()) {
                "Export succeeded but could not record completion."
            }
            target.uri
        }
}
