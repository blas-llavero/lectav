/*
 * LECTAV — local, stylized talking-avatar MP4 (AVC + AAC).
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

/** First iteration: original 2D illustration and approximate captions, not 3D/lip alignment. */
internal object OfflineAvatarVideo {
    private const val WIDTH=320
    private const val HEIGHT=240
    private const val FPS=5
    private const val MAX_SECONDS=600

    private data class Pcm(val rate:Int,val offset:Long,val length:Long) {
        val durationUs get()=length*1_000_000L/(rate*2L)
    }

    private fun wav(file:File):Pcm=RandomAccessFile(file,"r").use { r ->
        fun tag():String=ByteArray(4).also { r.readFully(it) }.toString(Charsets.US_ASCII)
        fun intLE()=Integer.reverseBytes(r.readInt()).toLong() and 0xffffffffL
        fun shortLE()=java.lang.Short.reverseBytes(r.readShort()).toInt() and 65535
        require(tag()=="RIFF")
        intLE()
        require(tag()=="WAVE")
        var rate=0
        var valid=false
        var offset=-1L
        var len=0L
        while(r.filePointer+8<=r.length()) {
            val name=tag()
            val size=intLE()
            val pos=r.filePointer
            require(size<=r.length()-pos) { "WAV incomplet." }
            when(name) {
                "fmt " -> {
                    require(size>=16)
                    val codec=shortLE()
                    val channels=shortLE()
                    rate=intLE().toInt()
                    r.skipBytes(6)
                    val bits=shortLE()
                    valid=codec==1 && channels==1 && bits==16
                }
                "data" -> { offset=pos;len=size;break }
            }
            r.seek(pos+size+(size%2))
        }
        require(valid && rate in setOf(8000,11025,12000,16000,22050,24000,32000,44100,48000))
        require(offset>=0 && len>=rate && len%2==0L)
        val pcm=Pcm(rate,offset,len)
        require(pcm.durationUs<=MAX_SECONDS*1_000_000L) { "Límit inicial: 10 minuts d'avatar." }
        pcm
    }

    private fun draw(canvas:Canvas,title:String,caption:String,talking:Boolean) {
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(17,31,50))
        paint.color=Color.rgb(48,110,151)
        canvas.drawRoundRect(RectF(54f,12f,266f,228f),22f,22f,paint)
        paint.color=Color.rgb(239,192,158)
        canvas.drawOval(RectF(98f,34f,222f,175f),paint)
        paint.color=Color.rgb(37,38,47)
        canvas.drawCircle(134f,96f,6f,paint)
        canvas.drawCircle(186f,96f,6f,paint)
        paint.color=Color.rgb(131,49,60)
        canvas.drawRoundRect(
            if(talking) RectF(144f,130f,176f,153f)
            else RectF(144f,140f,176f,146f),7f,7f,paint)
        paint.color=Color.WHITE
        paint.textAlign=Paint.Align.CENTER
        paint.textSize=15f
        canvas.drawText("LECTAV",160f,27f,paint)
        paint.textSize=12f
        val captionLines=caption.split(" ").fold(mutableListOf<String>()) { list,word ->
            if(list.isEmpty() || list.last().length+word.length+1>43) list.add(word)
            else list[list.lastIndex]=list.last()+" "+word
            list
        }
        captionLines.takeLast(2).forEachIndexed { index,line ->
            canvas.drawText(line.take(43),160f,193f+index*16f,paint)
        }
    }

    private fun yuv420(bitmap:Bitmap,semi:Boolean):ByteArray {
        val px=IntArray(WIDTH*HEIGHT)
        bitmap.getPixels(px,0,WIDTH,0,0,WIDTH,HEIGHT)
        val output=ByteArray(WIDTH*HEIGHT*3/2)
        val uv=WIDTH*HEIGHT
        for(y in 0 until HEIGHT) for(x in 0 until WIDTH) {
            val c=px[y*WIDTH+x]
            val r=Color.red(c);val g=Color.green(c);val b=Color.blue(c)
            output[y*WIDTH+x]=(((66*r+129*g+25*b+128 shr 8)+16).coerceIn(0,255)).toByte()
            if(y%2==0 && x%2==0) {
                val u=(((-38*r-74*g+112*b+128 shr 8)+128).coerceIn(0,255)).toByte()
                val v=(((112*r-94*g-18*b+128 shr 8)+128).coerceIn(0,255)).toByte()
                val i=(y/2)*(WIDTH/2)+x/2
                if(semi) {output[uv+2*i]=u;output[uv+2*i+1]=v}
                else {output[uv+i]=u;output[uv+WIDTH*HEIGHT/4+i]=v}
            }
        }
        return output
    }

    private suspend fun encodeVideo(picture:File,source:File,pcm:Pcm,script:String) {
        val encoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val caps=encoder.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
            .colorFormats.toSet()
        val semi=MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        val planar=MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        val color=when {
            semi in caps -> semi
            planar in caps -> planar
            else -> error("No hi ha codificador H.264 YUV420 compatible.")
        }
        val fmt=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,WIDTH,HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,color)
            setInteger(MediaFormat.KEY_BIT_RATE,350_000)
            setInteger(MediaFormat.KEY_FRAME_RATE,FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2)
        }
        val mux=MediaMuxer(picture.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val bufferInfo=MediaCodec.BufferInfo()
        var started=false
        var track=-1
        var ended=false
        val bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val captions=AvatarCaptions.fromText(script,pcm.durationUs)
        fun drain(wait:Boolean) {
            var retries=0
            while(true) {
                val index=encoder.dequeueOutputBuffer(bufferInfo,if(wait)10000 else 0)
                when(index) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!started)
                        track=mux.addTrack(encoder.outputFormat)
                        mux.start();started=true
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if(!wait)return
                        check(++retries<3000) { "Timeout H.264." }
                    }
                    else -> if(index>=0) {
                        if(bufferInfo.size>0) {
                            check(started)
                            val bytes=encoder.getOutputBuffer(index) ?: error("No output AVC")
                            bytes.position(bufferInfo.offset)
                            bytes.limit(bufferInfo.offset+bufferInfo.size)
                            mux.writeSampleData(track,bytes,bufferInfo)
                        }
                        ended=bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                        encoder.releaseOutputBuffer(index,false)
                        if(ended || !wait)return
                    }
                }
            }
        }
        try {
            encoder.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            val count=((pcm.durationUs*FPS+999_999)/1_000_000).toInt()
            RandomAccessFile(source,"r").use { audio ->
                for(frame in 0 until count) {
                    currentCoroutineContext().ensureActive()
                    val us=frame*1_000_000L/FPS
                    val sample=(us*pcm.rate/1_000_000L)*2L
                    val position=pcm.offset+min(sample,pcm.length-2)
                    audio.seek(position)
                    var peak=0
                    repeat(min(256L,(pcm.offset+pcm.length-position)/2L).toInt()) {
                        peak=maxOf(peak,abs(java.lang.Short.reverseBytes(audio.readShort()).toInt()))
                    }
                    val caption=captions.firstOrNull { us>=it.startUs && us<it.endUs }?.text.orEmpty()
                    draw(canvas,"LECTAV",caption,AvatarCaptions.mouthOpen(peak))
                    val bytes=yuv420(bitmap,color==semi)
                    var submitted=false
                    while(!submitted) {
                        currentCoroutineContext().ensureActive()
                        val slot=encoder.dequeueInputBuffer(10000)
                        if(slot>=0) {
                            val input=encoder.getInputBuffer(slot) ?: error("No AVC input")
                            input.clear()
                            require(input.remaining()>=bytes.size)
                            input.put(bytes)
                            encoder.queueInputBuffer(slot,0,bytes.size,us,0)
                            submitted=true
                        }
                        drain(false)
                    }
                    drain(false)
                }
            }
            var submitted=false
            while(!submitted) {
                currentCoroutineContext().ensureActive()
                val slot=encoder.dequeueInputBuffer(10000)
                if(slot>=0) {
                    encoder.queueInputBuffer(slot,0,0,pcm.durationUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    submitted=true
                }
                drain(false)
            }
            drain(true)
            check(ended && started)
        } finally {
            runCatching { encoder.stop() }
            encoder.release()
            if(started)runCatching { mux.stop() }
            mux.release()
            bitmap.recycle()
        }
    }

    private suspend fun encodeAudio(audioFile:File,source:File,pcm:Pcm) {
        val encoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val fmt=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,pcm.rate,1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE,64000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,8192)
        }
        val mux=MediaMuxer(audioFile.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val info=MediaCodec.BufferInfo()
        var started=false
        var track=-1
        var ended=false
        fun drain(wait:Boolean) {
            var retries=0
            while(true) {
                val index=encoder.dequeueOutputBuffer(info,if(wait)10000 else 0)
                when(index) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!started)
                        track=mux.addTrack(encoder.outputFormat)
                        mux.start();started=true
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if(!wait)return
                        check(++retries<3000) { "Timeout AAC." }
                    }
                    else -> if(index>=0) {
                        if(info.size>0) {
                            check(started)
                            val bytes=encoder.getOutputBuffer(index) ?: error("No output AAC")
                            bytes.position(info.offset);bytes.limit(info.offset+info.size)
                            mux.writeSampleData(track,bytes,info)
                        }
                        ended=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                        encoder.releaseOutputBuffer(index,false)
                        if(ended || !wait)return
                    }
                }
            }
        }
        try {
            encoder.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            RandomAccessFile(source,"r").use { input ->
                input.seek(pcm.offset)
                var offset=0L
                var eof=false
                while(!eof) {
                    currentCoroutineContext().ensureActive()
                    val slot=encoder.dequeueInputBuffer(10000)
                    if(slot>=0) {
                        val bytes=encoder.getInputBuffer(slot) ?: error("No AAC input")
                        bytes.clear()
                        val n=min(min(bytes.remaining().toLong(),8192L),pcm.length-offset).toInt() and -2
                        if(n>0) {
                            val chunk=ByteArray(n)
                            input.readFully(chunk)
                            bytes.put(chunk)
                            encoder.queueInputBuffer(slot,0,n,
                                offset*1_000_000L/(pcm.rate*2L),0)
                            offset+=n
                        } else {
                            encoder.queueInputBuffer(slot,0,0,pcm.durationUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eof=true
                        }
                    }
                    drain(false)
                }
            }
            drain(true)
            check(ended && started)
        } finally {
            runCatching { encoder.stop() }
            encoder.release()
            if(started)runCatching { mux.stop() }
            mux.release()
        }
    }

    private suspend fun muxTracks(video:File,audio:File,dest:File) {
        val mux=MediaMuxer(dest.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val v=MediaExtractor()
        val a=MediaExtractor()
        var started=false
        try {
            v.setDataSource(video.absolutePath)
            a.setDataSource(audio.absolutePath)
            require(v.trackCount==1 && a.trackCount==1)
            require(v.getTrackFormat(0).getString(MediaFormat.KEY_MIME)
                ?.startsWith("video/")==true)
            require(a.getTrackFormat(0).getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/")==true)
            val vTrack=mux.addTrack(v.getTrackFormat(0))
            val aTrack=mux.addTrack(a.getTrackFormat(0))
            mux.start();started=true
            val buffer=ByteBuffer.allocateDirect(1024*1024)
            for((extractor,destination) in listOf(v to vTrack,a to aTrack)) {
                extractor.selectTrack(0)
                while(true) {
                    currentCoroutineContext().ensureActive()
                    buffer.clear()
                    val length=extractor.readSampleData(buffer,0)
                    if(length<0)break
                    require(length in 1..buffer.capacity())
                    val info=MediaCodec.BufferInfo()
                    info.set(0,length,extractor.sampleTime,extractor.sampleFlags)
                    mux.writeSampleData(destination,buffer,info)
                    if(!extractor.advance())break
                }
                extractor.unselectTrack(0)
            }
        } finally {
            v.release();a.release()
            if(started)runCatching { mux.stop() }
            mux.release()
        }
    }

    private fun verify(file:File) {
        require(file.length() > 1024) { "Vídeo MP4 buit." }
        val reader=MediaExtractor()
        try {
            reader.setDataSource(file.absolutePath)
            require(reader.trackCount>=2) { "MP4 sense dos fluxos." }
            val mimes=(0 until reader.trackCount).map {
                reader.getTrackFormat(it).getString(MediaFormat.KEY_MIME).orEmpty()
            }
            require(mimes.any { it.startsWith("audio/") } &&
                mimes.any { it.startsWith("video/") }) {
                "MP4 sense vídeo i àudio."
            }
        } finally {reader.release()}
    }

    suspend fun create(context:Context,audioUri:String,script:String):String =
        withContext(Dispatchers.IO) {
            require(script.length in 10..50_000)
            val temporary=File(context.cacheDir,"avatar_"+UUID.randomUUID())
            check(temporary.mkdirs())
            val folder=File(context.filesDir,"generated").apply {
                check(isDirectory || mkdirs())
            }
            val output=File(folder,"avatar_"+UUID.randomUUID()+".mp4")
            try {
                val source=File(temporary,"source.wav")
                context.contentResolver.openInputStream(Uri.parse(audioUri)).use { input ->
                    require(input!=null)
                    source.outputStream().use { target ->
                        val buffer=ByteArray(65536)
                        var copied=0L
                        while(true) {
                            currentCoroutineContext().ensureActive()
                            val n=input.read(buffer)
                            if(n<0)break
                            copied+=n
                            require(copied<=100L*1024*1024) { "WAV massa gran." }
                            target.write(buffer,0,n)
                        }
                    }
                }
                val pcm=wav(source)
                val picture=File(temporary,"video.mp4")
                val sound=File(temporary,"audio.mp4")
                encodeVideo(picture,source,pcm,script)
                encodeAudio(sound,source,pcm)
                muxTracks(picture,sound,output)
                verify(output)
                FileProvider.getUriForFile(context,context.packageName+".files",output).toString()
            } catch(e:Exception) {
                output.delete()
                throw e
            } finally {
                temporary.deleteRecursively()
            }
        }
}
