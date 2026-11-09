/*
 * LECTAV — on-device offline TextToSpeech via the GPL Piper Android engine.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.media.AudioFormat
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.FileProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * The explicit Piper app ID prevents falling back to closed/cloud Android TTS.
 * No audio or script leaves the device; native Android TTS writes PCM WAV.
 */
internal object OnDeviceSpeech {
    private const val PIPER_PACKAGE = "dev.ihorshevchuk.piper"
    private const val MAX_TEXT = 8000

    data class OfflineVoice(val name: String, val label: String)

    private suspend fun <T> withPiper(
        context: Context, action: suspend (TextToSpeech) -> T
    ): T = withContext(Dispatchers.Main) {
        val ready = CompletableDeferred<Int>()
        val tts = TextToSpeech(context.applicationContext, { status ->
            ready.complete(status)
        }, PIPER_PACKAGE)
        try {
            check(withTimeout(30000) { ready.await() } == TextToSpeech.SUCCESS) {
                "Instal·la Piper Android, configura una veu i torna-ho a provar."
            }
            check(tts.defaultEngine == PIPER_PACKAGE) {
                "No s'ha activat Piper. No es farà servir cap motor alternatiu."
            }
            action(tts)
        } finally {
            tts.stop()
            tts.shutdown()
        }
    }

    suspend fun voices(context: Context): List<OfflineVoice> = withPiper(context) { tts ->
        tts.voices.orEmpty()
            .filter { !it.isNetworkConnectionRequired }
            .sortedWith(compareBy<Voice> { it.locale.language }.thenBy { it.name })
            .map { OfflineVoice(it.name, "${it.locale.toLanguageTag()} · ${it.name}") }
    }

    private fun chunks(text: String, max: Int = 650): List<String> {
        val words = text.trim().split(Regex("""\s+"""))
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        for(word in words) {
            require(word.length <= max) { "Fragment massa llarg. Divideix el guió en frases." }
            if(current.isNotEmpty() && current.length + 1 + word.length > max) {
                parts.add(current.toString())
                current = StringBuilder()
            }
            if(current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if(current.isNotEmpty()) parts.add(current.toString())
        return parts
    }

    private fun turns(script: String, podcast: Boolean): List<Pair<Boolean, String>> {
        if (!podcast) return chunks(script).map { false to it }
        val lines = script.lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty() && lines.all { Regex("""^[AB]:\s*\S.*""").matches(it.trim()) }) {
            "Cada intervenció del podcast ha de començar amb A: o B:."
        }
        return lines.flatMap { line ->
            val trimmed = line.trim()
            chunks(trimmed.substring(2)).map { (trimmed.startsWith("B:")) to it }
        }
    }

    private suspend fun speakToFile(tts: TextToSpeech, text: String, file: File) {
        val pending = CompletableDeferred<Unit>()
        val id = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) {
                if(utteranceId == id) pending.complete(Unit)
            }
            override fun onError(utteranceId: String) {
                if(utteranceId == id) pending.completeExceptionally(
                    IllegalStateException("Piper no ha pogut sintetitzar aquest fragment.")
                )
            }
        })
        require(tts.synthesizeToFile(text,Bundle(),file,id) == TextToSpeech.SUCCESS) {
            "Piper no ha acceptat la síntesi."
        }
        withTimeout(180000) { pending.await() }
        require(file.length() > 44) { "La veu instal·lada ha generat un fitxer buit." }
    }

    private data class WavPart(val file: File, val rate: Int, val channels: Int,
                               val bits: Int, val offset: Long, val length: Long)

    /** Accept only ordinary RIFF PCM WAV: never silently concatenate incompatible voices. */
    private fun wavInfo(file: File): WavPart {
        RandomAccessFile(file,"r").use { reader ->
            require(reader.length() >= 44)
            fun word(): Int = Integer.reverseBytes(reader.readInt())
            val riff = ByteArray(4)
            reader.readFully(riff)
            require(String(riff,Charsets.US_ASCII) == "RIFF")
            reader.skipBytes(4)
            reader.readFully(riff)
            require(String(riff,Charsets.US_ASCII) == "WAVE")
            var format = 0
            var channels = 0
            var rate = 0
            var bits = 0
            var dataOffset = -1L
            var dataLength = 0L
            while(reader.filePointer + 8 <= reader.length()) {
                reader.readFully(riff)
                val len = word().toLong() and 0xffffffffL
                val start = reader.filePointer
                require(len <= reader.length()-start) { "WAV malmès." }
                when(String(riff,Charsets.US_ASCII)) {
                    "fmt " -> {
                        require(len >= 16)
                        format = java.lang.Short.reverseBytes(reader.readShort()).toInt() and 0xffff
                        channels = java.lang.Short.reverseBytes(reader.readShort()).toInt() and 0xffff
                        rate = word()
                        reader.skipBytes(6)
                        bits = java.lang.Short.reverseBytes(reader.readShort()).toInt() and 0xffff
                    }
                    "data" -> { dataOffset = start; dataLength = len; break }
                }
                reader.seek(start+len+(len%2))
            }
            require(format==1 && channels==1 && bits==16 && rate in 8000..48000 && dataOffset>=0 && dataLength>0) {
                "Piper ha retornat un format de WAV no compatible (cal PCM mono de 16 bits)."
            }
            return WavPart(file,rate,channels,bits,dataOffset,dataLength)
        }
    }

    private fun saveWave(parts: List<WavPart>, dest: File) {
        require(parts.isNotEmpty() && parts.all { it.rate==parts[0].rate &&
            it.bits==parts[0].bits && it.channels==parts[0].channels }) {
            "Les dues veus han de tenir la mateixa freqüència de mostreig. Tria veus compatibles."
        }
        val len = parts.sumOf { it.length }
        require(len in 1..(60L*1024*1024))
        RandomAccessFile(dest,"rw").use { out ->
            fun label(s: String) { out.write(s.toByteArray(Charsets.US_ASCII)) }
            fun intLE(v: Int) { out.writeInt(Integer.reverseBytes(v)) }
            fun shortLE(v: Int) { out.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt()) }
            label("RIFF"); intLE((36+len).toInt()); label("WAVE")
            label("fmt "); intLE(16); shortLE(1); shortLE(1)
            intLE(parts[0].rate); intLE(parts[0].rate*2)
            shortLE(2); shortLE(16)
            label("data"); intLE(len.toInt())
            val buffer = ByteArray(8192)
            parts.forEach { p ->
                RandomAccessFile(p.file,"r").use { input ->
                    input.seek(p.offset)
                    var remain = p.length
                    while(remain>0) {
                        val n = input.read(buffer,0,minOf(remain,buffer.size.toLong()).toInt())
                        check(n>0)
                        out.write(buffer,0,n)
                        remain-=n
                    }
                }
            }
        }
    }

    suspend fun synthesize(
        context: Context, script: String, mode: String,
        voiceA: String, voiceB: String
    ): String {
        require(script.length in 2..MAX_TEXT && mode in setOf("audio","podcast"))
        return withPiper(context) { tts ->
            withContext(Dispatchers.IO) {
                val voices = tts.voices.orEmpty().filter { !it.isNetworkConnectionRequired }
                val a = voices.firstOrNull { it.name==voiceA }
                    ?: error("Selecciona una veu Piper instal·lada.")
                val b = if(mode=="podcast") voices.firstOrNull { it.name==voiceB }
                    ?: error("Selecciona una segona veu Piper instal·lada.")
                    else a
                val scriptParts=turns(script,mode=="podcast")
                require(scriptParts.size <= 45) { "Guió massa llarg; divideix-lo en sessions." }
                val temp = File(context.cacheDir,"tts_${UUID.randomUUID()}").apply { check(mkdirs()) }
                val output = File(context.filesDir,"generated").apply {
                    check(isDirectory || mkdirs())
                }.resolve("tts_${UUID.randomUUID()}.wav")
                try {
                    val pieces=mutableListOf<WavPart>()
                    scriptParts.forEachIndexed { i, (second,text) ->
                        coroutineContext.ensureActive()
                        val voice=if(second) b else a
                        require(tts.setVoice(voice)==TextToSpeech.SUCCESS) {
                            "No s'ha pogut activar la veu Piper seleccionada."
                        }
                        val file=File(temp,"part_${i}.wav")
                        speakToFile(tts,text,file)
                        pieces.add(wavInfo(file))
                    }
                    saveWave(pieces,output)
                    FileProvider.getUriForFile(
                        context,context.packageName+".files",output
                    ).toString()
                } catch(e: Exception) {
                    output.delete()
                    throw e
                } finally {
                    temp.deleteRecursively()
                }
            }
        }
    }
}
