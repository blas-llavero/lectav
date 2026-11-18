package com.blasllavero.lectav

import android.content.Context
import androidx.core.content.FileProvider
import com.CodeBySonu.VoxSherpa.VoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** GPL speech in LECTAV's own process. Models are downloaded on demand,
 * never bundled, re-uploaded or sent to a service. */
internal object EmbeddedSpeech {
    private const val HOST="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"
    data class Voice(val id:String,val label:String,val file:String)
    val catalog=listOf(
        Voice("ca-ona","Català · Ona (INT8)","vits-piper-ca_ES-upc_ona-x_low-int8.tar.bz2"),
        Voice("ca-pau","Català · Pau (INT8)","vits-piper-ca_ES-upc_pau-x_low-int8.tar.bz2"),
        Voice("es-miro","Castellà · Miro (INT8)","vits-piper-es_ES-miro-high-int8.tar.bz2"),
        Voice("es-sharvard","Castellà · Sharvard (INT8)","vits-piper-es_ES-sharvard-medium-int8.tar.bz2")
    )
    private fun voiceDir(ctx:Context,id:String)=File(ctx.filesDir,"embedded-tts/voice-"+id)
    private fun espeak(ctx:Context)=File(ctx.filesDir,"espeak-ng-data")
    fun preferredVoiceId(ctx:Context):String =
        ctx.getSharedPreferences("lectav_generator",Context.MODE_PRIVATE)
            .getString("preferredSpeechVoice","ca-ona") ?: "ca-ona"
    fun setPreferredVoiceId(ctx:Context,id:String) {
        require(catalog.any{it.id==id})
        check(ctx.getSharedPreferences("lectav_generator",Context.MODE_PRIVATE)
            .edit().putString("preferredSpeechVoice",id).commit())
    }
    private fun model(dir:File):Pair<File,File>? {
        if(!dir.isDirectory)return null
        val fs=dir.walkTopDown().maxDepth(5).filter{it.isFile}.toList()
        val onnx=fs.firstOrNull{it.name.endsWith(".onnx") && it.length()>1024}?:return null
        val tok=fs.firstOrNull{it.name=="tokens.txt" && it.length()>0}?:return null
        return onnx to tok
    }
    fun installed(ctx:Context)=catalog.filter{
        model(voiceDir(ctx,it.id))!=null && espeak(ctx).resolve("phontab").isFile
    }

    private suspend fun download(url:String,dest:File)=withContext(Dispatchers.IO){
        val conn=(URL(url).openConnection() as HttpURLConnection).apply{
            connectTimeout=20000;readTimeout=30000;instanceFollowRedirects=true
        }
        try {
            check(conn.responseCode==200){"Descàrrega fallida HTTP "+conn.responseCode}
            var nTotal=0L
            conn.inputStream.use{input->dest.outputStream().use{out->
                val bytes=ByteArray(65536)
                while(true){
                    coroutineContext.ensureActive()
                    val n=input.read(bytes)
                    if(n<0)break
                    nTotal+=n
                    require(nTotal<=120L*1024*1024){"Descàrrega excessiva."}
                    out.write(bytes,0,n)
                }
            }}
            require(nTotal>1000)
        } finally {conn.disconnect()}
    }

    private suspend fun expand(archive:File,dest:File)=withContext(Dispatchers.IO){
        check(dest.isDirectory || dest.mkdirs())
        val root=dest.canonicalFile.toPath()
        var total=0L
        BZip2CompressorInputStream(archive.inputStream().buffered()).use{bz->
            TarArchiveInputStream(bz).use{tar->
                while(true){
                    coroutineContext.ensureActive()
                    val e=tar.nextTarEntry?:break
                    require(!e.isLink && !e.isSymbolicLink)
                    require(!e.name.startsWith("/") && !e.name.contains('\\') &&
                        e.name.split('/').none{it==".."})
                    val f=File(dest,e.name).canonicalFile
                    require(f.toPath().startsWith(root))
                    if(e.isDirectory){check(f.isDirectory || f.mkdirs());continue}
                    if(!e.isFile)continue
                    total+=e.size
                    require(e.size>=0 && total<=250L*1024*1024)
                    f.parentFile?.mkdirs()
                    f.outputStream().use{out->
                        val bytes=ByteArray(65536)
                        var remain=e.size
                        while(remain>0){
                            coroutineContext.ensureActive()
                            val n=tar.read(bytes,0,minOf(remain,bytes.size.toLong()).toInt())
                            require(n>0)
                            out.write(bytes,0,n)
                            remain-=n
                        }
                    }
                }
            }
        }
    }

    suspend fun install(ctx:Context,id:String)=withContext(Dispatchers.IO){
        val voice=catalog.firstOrNull{it.id==id}?:error("Veu desconeguda")
        val temp=File(ctx.cacheDir,"voice-"+UUID.randomUUID())
        check(temp.mkdirs())
        try{
            if(!espeak(ctx).resolve("phontab").isFile){
                val compressed=File(temp,"espeak.tar.bz2")
                download(HOST+"espeak-ng-data.tar.bz2",compressed)
                val expanded=File(temp,"espeak")
                expand(compressed,expanded)
                val correct=expanded.walkTopDown().maxDepth(6).firstOrNull{
                    it.isDirectory && File(it,"phontab").isFile
                }?:error("Dades de pronunciació incompletes.")
                val staged=File(ctx.filesDir,"espeak-ng-data.tmp")
                staged.deleteRecursively()
                check(correct.copyRecursively(staged,overwrite=true))
                espeak(ctx).deleteRecursively()
                check(staged.renameTo(espeak(ctx)))
            }
            val compressed=File(temp,"model.tar.bz2")
            download(HOST+voice.file,compressed)
            val expanded=File(temp,"model")
            expand(compressed,expanded)
            require(model(expanded)!=null){"Model sense ONNX/tokens."}
            val target=voiceDir(ctx,id)
            val staged=File(target.parentFile,"voice-"+id+".tmp")
            staged.deleteRecursively()
            check(expanded.copyRecursively(staged,overwrite=true))
            target.deleteRecursively()
            check(staged.renameTo(target))
        }finally{temp.deleteRecursively()}
    }

    private fun chunks(original:String):List<String>{
        val txt=MathSpeechNormalizer.normalize(original)
        require(txt.length in 10..50000){"Guió: de 10 a 50.000 caràcters."}
        val chunks=mutableListOf<String>()
        var current=""
        for(word in txt.split(" ")){
            if(current.isNotBlank() && current.length+word.length+1>280){
                chunks.add(current);current=""
            }
            current=if(current.isBlank())word else current+" "+word
        }
        if(current.isNotBlank())chunks.add(current)
        require(chunks.size<=250)
        return chunks
    }

    suspend fun synthesize(ctx:Context,script:String,id:String):String=
        withContext(Dispatchers.IO){
            require(catalog.any{it.id==id})
            require(espeak(ctx).resolve("phontab").isFile){"Descarrega una veu a LECTAV."}
            val files=model(voiceDir(ctx,id))?:error("Veu no instal·lada.")
            val engine=VoiceEngine()
            val output=File(ctx.filesDir,"generated").apply{
                check(isDirectory || mkdirs())
            }.resolve("tts_"+UUID.randomUUID()+".wav")
            try{
                val status=engine.loadModel(ctx,files.first.absolutePath,files.second.absolutePath)
                require(status=="Success"){"Error motor de veu: "+status}
                val sampleRate=engine.sampleRate
                require(sampleRate in 8000..48000) { "Freqüència de veu no vàlida." }
                RandomAccessFile(output,"rw").use{wav->
                    wav.setLength(0);wav.write(ByteArray(44))
                    var count=0L
                    for(part in chunks(script)){
                        coroutineContext.ensureActive()
                        val pcm=engine.generateAudioPCM(part,1.0f,1.0f)
                            ?:error("No s'ha generat un fragment de veu.")
                        require(pcm.isNotEmpty() && pcm.size%2==0)
                        count+=pcm.size
                        require(count<=350L*1024*1024)
                        wav.write(pcm)
                    }
                    require(count>0)
                    wav.seek(0)
                    fun label(v:String){wav.write(v.toByteArray(Charsets.US_ASCII))}
                    fun n(v:Int){wav.writeInt(Integer.reverseBytes(v))}
                    fun sh(v:Int){wav.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())}
                    label("RIFF");n((36+count).toInt());label("WAVE")
                    label("fmt ");n(16);sh(1);sh(1)
                    n(sampleRate);n(sampleRate*2);sh(2);sh(16)
                    label("data");n(count.toInt())
                }
                FileProvider.getUriForFile(ctx,ctx.packageName+".files",output).toString()
            }catch(e:Exception){output.delete();throw e}
            finally{engine.destroy()}
        }
}
