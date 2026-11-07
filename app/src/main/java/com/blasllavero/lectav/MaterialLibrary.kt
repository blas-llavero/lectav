package com.blasllavero.lectav

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.lectavFiles by preferencesDataStore("lectav_files")

internal data class LocalMaterial(
    val id: String, val courseKey: String, val uri: String,
    val name: String, val type: String, val opened: Boolean, val completed: Boolean,
    val origin: String = "imported",
    val lastExportUri: String = "",
    val sessionKey: String = ""
)

internal fun courseKey(course: SavedCourse) =
    "ES-CT|"+course.pathway+"|"+course.course+"|"+course.subject

internal class MaterialLibrary(context: Context) {
    private val store = context.applicationContext.lectavFiles
    private val key = stringPreferencesKey("files_json")
    private fun decode(json: String): List<LocalMaterial> = runCatching {
        val arr = JSONArray(json)
        (0 until minOf(arr.length(), 500)).mapNotNull { i ->
            val j = arr.optJSONObject(i) ?: return@mapNotNull null
            val uri = j.optString("uri")
            if (!uri.startsWith("content://")) return@mapNotNull null
            LocalMaterial(j.optString("id"), j.optString("courseKey"), uri,
                j.optString("name"), j.optString("type"),
                j.optBoolean("opened"), j.optBoolean("completed"),
                j.optString("origin", "imported"), j.optString("lastExportUri"),
                j.optString("sessionKey"))
        }
    }.getOrDefault(emptyList())
    private fun encode(items: List<LocalMaterial>): String {
        val arr = JSONArray()
        items.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("courseKey",m.courseKey)
                put("uri",m.uri); put("name",m.name); put("type",m.type)
                put("opened",m.opened); put("completed",m.completed)
                put("origin",m.origin)
                put("lastExportUri",m.lastExportUri)
                put("sessionKey",m.sessionKey)
            })
        }
        return arr.toString()
    }
    val files = store.data.map { decode(it[key] ?: "[]") }
    suspend fun restoreBackup(items: List<LocalMaterial>) {
        require(items.size <= 500)
        require(items.all {
            it.id.isNotBlank() && it.courseKey.length in 1..200 &&
                it.uri.startsWith("content://") &&
                it.name.length in 1..160 &&
                it.type in listOf("lesson", "exercise", "solutions", "video") &&
                it.origin in listOf("generated", "restored") &&
                it.sessionKey.length <= 150
        }) { "La biblioteca restaurada conté elements invàlids." }
        store.edit { p ->
            p[key] = encode(items.distinctBy { it.id })
        }
    }

    suspend fun add(course: SavedCourse, uri: String, name: String, type: String,
        origin: String = "imported", sessionKey: String = "") {
        require(origin in listOf("imported","generated","restored"))
        require(type in listOf("lesson","exercise","solutions","video"))
        require(uri.startsWith("content://"))
        require(sessionKey.length <= 150)
        store.edit { p ->
            val items=decode(p[key] ?: "[]")
            if(items.size >= 500 || items.any {
                it.courseKey==courseKey(course) && it.uri==uri && it.type==type
            }) return@edit
            p[key]=encode(items+LocalMaterial(UUID.randomUUID().toString(),
                courseKey(course),uri,name.take(160),type,false,false,origin,
                sessionKey=sessionKey))
        }
    }
    suspend fun update(id: String, opened: Boolean? = null, completed: Boolean? = null) {
        store.edit { p ->
            p[key]=encode(decode(p[key] ?: "[]").map { m ->
                if(m.id==id) m.copy(opened=opened ?: m.opened,
                    completed=completed ?: m.completed) else m
            })
        }
    }
    suspend fun recordExport(id: String, destination: String) {
        require(destination.startsWith("content://"))
        store.edit { p ->
            p[key] = encode(decode(p[key] ?: "[]").map { item ->
                if (item.id == id) item.copy(lastExportUri = destination) else item
            })
        }
    }
    suspend fun remove(id: String) {
        store.edit { p ->
            p[key]=encode(decode(p[key] ?: "[]").filterNot { it.id==id })
        }
    }
}
