package com.blasllavero.lectav

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MaterialsScreen(
    course: SavedCourse,
    files: List<LocalMaterial>,
    onAdd: (String,String,String) -> Unit,
    onOpen: (String) -> Unit,
    onUpdate: (String,Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onGenerate: () -> Unit,
    onExported: (String, String) -> Unit
) {
    val context=LocalContext.current
    var kind by remember { mutableStateOf("lesson") }
    var importError by remember { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<String?>(null) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            importError=runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val resolver=context.contentResolver
                val cursor=resolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)
                val name=cursor?.use { c ->
                    if(c.moveToFirst()) c.getString(0) else null
                } ?: "Document"
                val mime=resolver.getType(uri).orEmpty()
                val valid=if(kind=="video") mime=="video/mp4" || name.endsWith(".mp4",true)
                    else mime=="application/pdf" || name.endsWith(".pdf",true)
                if(!valid) error("Invalid document type")
                onAdd(uri.toString(),name,kind)
            }.isFailure
        }
    }
    Text(stringResource(R.string.materials_title),style=MaterialTheme.typography.titleLarge)
    Text(
        "Els PDF generats es guarden primer a la memòria privada de LECTAV. " +
        "Per veure'ls a CX File Explorer o guardar-los a la targeta SD, " +
        "prem «Desa una còpia a una carpeta…» i escull on vols conservar-los.",
        style=MaterialTheme.typography.bodySmall
    )
    Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
        Text("Genera teoria, exercicis o solucionari")
    }
    Text(stringResource(R.string.materials_intro),style=MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick={kind="lesson";picker.launch(arrayOf("application/pdf"))}) {
        Text(stringResource(R.string.material_import_lesson))
    }
    OutlinedButton(onClick={kind="exercise";picker.launch(arrayOf("application/pdf"))}) {
        Text(stringResource(R.string.material_import_exercise))
    }
    OutlinedButton(onClick={kind="video";picker.launch(arrayOf("video/mp4"))}) {
        Text(stringResource(R.string.material_import_video))
    }
    if(importError) Text(stringResource(R.string.material_error),color=MaterialTheme.colorScheme.error)
    if(files.isEmpty()) Text(stringResource(R.string.materials_empty))
    files.forEach { file ->
        Card(modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(file.name,style=MaterialTheme.typography.titleMedium)
                Text(stringResource(when(file.type) {
                    "video"->R.string.material_kind_video
                    "exercise"->R.string.material_kind_exercise
                    else->R.string.material_kind_lesson
                }),style=MaterialTheme.typography.bodySmall)
                if(course.role=="student") {
                    Text(stringResource(when {
                        file.completed->R.string.material_completed
                        file.opened->R.string.material_opened
                        else->R.string.material_not_opened
                    }))
                }
                if (file.origin == "generated") {
                    Text("PDF creat amb LECTAV · esborrany")
                }
                Text(
                    MaterialLocation.source(context, file),
                    style=MaterialTheme.typography.bodySmall
                )
                if (file.lastExportUri.isNotBlank()) {
                    Text(
                        "Darrera còpia exportada: " +
                            MaterialLocation.selectedLocation(file.lastExportUri),
                        style=MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={onOpen(file.id)}) {Text(stringResource(R.string.library_open))}
                    OutlinedButton(onClick={
                        runCatching {
                            val intent=Intent(Intent.ACTION_SEND).apply {
                                type = if(file.type=="video") "video/mp4" else "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(file.uri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent,"Comparteix material"))
                        }
                    }) { Text("Comparteix") }
                    TextButton(onClick={removeId=file.id}) {
                        Text(stringResource(R.string.library_remove))
                    }
                }
                ExportDocumentButton(
                    sourceUri = file.uri,
                    displayName = MaterialFileNames.forExport(file.name, file.type),
                    mime = materialMime(file.type),
                    onExported = { destination -> onExported(file.id, destination) }
                )
                if(course.role=="student" && file.type!="video") OutlinedButton(
                    onClick={onUpdate(file.id,!file.completed)}
                ) {
                    Text(stringResource(if(file.completed) R.string.material_mark_pending
                        else R.string.material_mark_complete))
                }
            }
        }
    }
    Text(stringResource(R.string.materials_storage_note),style=MaterialTheme.typography.bodySmall)
    if(removeId!=null) AlertDialog(
        onDismissRequest={removeId=null},
        title={Text(stringResource(R.string.material_remove_title))},
        text={Text(stringResource(R.string.material_remove_body))},
        confirmButton={TextButton(onClick={removeId?.let(onRemove);removeId=null}) {
            Text(stringResource(R.string.library_remove))
        }},
        dismissButton={TextButton(onClick={removeId=null}) {
            Text(stringResource(R.string.library_cancel))
        }}
    )
}

@Composable
internal fun MaterialViewer(file: LocalMaterial, onExported: (String) -> Unit = {}) {
    val context=LocalContext.current
    Text(file.name,style=MaterialTheme.typography.titleMedium)
    Text(MaterialLocation.source(context, file), style=MaterialTheme.typography.bodySmall)
    if (file.lastExportUri.isNotBlank()) {
        Text(
            "Darrera còpia exportada: " +
                MaterialLocation.selectedLocation(file.lastExportUri),
            style=MaterialTheme.typography.bodySmall
        )
    }
    ExportDocumentButton(
        sourceUri=file.uri,
        displayName=MaterialFileNames.forExport(file.name,file.type),
        mime=materialMime(file.type),
        onExported=onExported
    )
    if(file.type=="video") {
        var failed by remember(file.id){mutableStateOf(false)}
        val video=remember(file.id){
            VideoView(context).apply {
                setMediaController(MediaController(context))
                setOnErrorListener { _,_,_ ->failed=true;true }
            }
        }
        DisposableEffect(video) {
            video.setVideoURI(Uri.parse(file.uri))
            video.start()
            onDispose { video.stopPlayback() }
        }
        AndroidView(factory={video},modifier=Modifier.fillMaxWidth().height(290.dp))
        if(failed) Text(stringResource(R.string.material_open_error))
    } else {
        var pageIndex by remember(file.id){mutableIntStateOf(0)}
        var totalPages by remember(file.id){mutableIntStateOf(0)}
        var image by remember(file.id){mutableStateOf<Bitmap?>(null)}
        var error by remember(file.id){mutableStateOf(false)}
        LaunchedEffect(file.uri,pageIndex){
            image=null;error=false
            val result=runCatching {
                withContext(Dispatchers.IO){
                    val fd=context.contentResolver.openFileDescriptor(Uri.parse(file.uri),"r")
                        ?:kotlin.error("No PDF")
                    fd.use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            if(renderer.pageCount<=0) kotlin.error("Empty PDF")
                            val index=pageIndex.coerceIn(0,renderer.pageCount-1)
                            renderer.openPage(index).use { pdfPage ->
                                val w=900
                                val h=(900f*pdfPage.height/pdfPage.width.coerceAtLeast(1))
                                    .toInt().coerceIn(100,1600)
                                val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                pdfPage.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap to renderer.pageCount
                            }
                        }
                    }
                }
            }
            val pair=result.getOrNull()
            image=pair?.first
            totalPages=pair?.second ?:0
            error=result.isFailure
        }
        if(error) Text(stringResource(R.string.material_open_error),color=MaterialTheme.colorScheme.error)
        else if(image==null) CircularProgressIndicator()
        else {
            Text(stringResource(R.string.material_page_count,pageIndex+1,totalPages))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled=pageIndex>0,onClick={pageIndex--}) {
                    Text(stringResource(R.string.material_prev))
                }
                OutlinedButton(enabled=pageIndex<totalPages-1,onClick={pageIndex++}) {
                    Text(stringResource(R.string.material_next))
                }
            }
            Image(bitmap=image!!.asImageBitmap(),contentDescription=null,
                modifier=Modifier.fillMaxWidth())
        }
    }
}
