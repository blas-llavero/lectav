/*
 * LECTAV — offline Markdown/KaTeX PDF generation via Android WebView canvas.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import org.json.JSONObject
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.ceil

/**
 * Fully offline PDF conversion: the HTML, KaTeX, markdown-it and math fonts are
 * bundled in the APK. WebView does not load external URLs. Android PdfDocument
 * captures the typeset WebView on A4 canvas pages, rather than printing TeX
 * commands as literal strings.
 */
internal object TypesetPdfExporter {
    private const val TEMPLATE = "math/print-template.html"
    private const val VIEW_WIDTH = 794
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f
    // Limit expensive whole-document WebView drawing on modest Android tablets.
    private const val MAX_PAGES = 18
    private const val MAX_EXPORT_MS = 90_000L
    private val exportMutex = Mutex()
    private var slowWholeDocumentDrawEnabled = false

    private fun encode(s: String): String = Base64.encodeToString(
        s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
    )

    private class RenderReady(private val onReady: (String, String) -> Unit) {
        private val main = Handler(Looper.getMainLooper())

        @JavascriptInterface
        fun complete(error: String, layoutJson: String) {
            main.post { onReady(error.take(300), layoutJson.take(12000)) }
        }
    }

    suspend fun save(context: Context, documentTitle: String, body: String): Uri =
        withContext(Dispatchers.Main) {
            check(exportMutex.tryLock()) {
                "Ja hi ha un PDF en procés de desament. Espera que finalitzi."
            }
            val startTime = SystemClock.elapsedRealtime()
            try {
            require(body.isNotBlank()) { "No hi ha contingut per desar." }
            require(body.length <= 50_000) { "L'esborrany supera el límit d'aquest prototip." }

            // Android 5.0+: allow software drawing of the entire document,
            // not only the currently visible WebView viewport.
            if (!slowWholeDocumentDrawEnabled) {
                // Android requires this to run before constructing the first
                // PDF WebView; it is a process-wide setting, not per-export.
                WebView.enableSlowWholeDocumentDraw()
                slowWholeDocumentDrawEnabled = true
            }
            val browser = WebView(context)
            val fileFolder = File(context.filesDir, "generated").apply {
                check(isDirectory || mkdirs()) { "No es pot crear la carpeta de documents." }
            }
            try {
                browser.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                browser.settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    allowContentAccess = false
                    blockNetworkLoads = true
                    domStorageEnabled = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                }
                browser.webViewClient = object : WebViewClient() {}
                browser.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        VIEW_WIDTH, View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(
                        1123, View.MeasureSpec.EXACTLY
                    )
                )
                browser.layout(0, 0, VIEW_WIDTH, 1123)

                val layoutJson = withTimeout(60_000L) {
                    suspendCancellableCoroutine<String> { continuation ->
                        browser.addJavascriptInterface(RenderReady { error, layout ->
                            if (!continuation.isActive) return@RenderReady
                            if (error.isNotBlank()) {
                                continuation.resumeWithException(IOException(error))
                            } else if (layout.isBlank()) {
                                continuation.resumeWithException(
                                    IOException("No s'han rebut els salts de pàgina.")
                                )
                            } else {
                                continuation.resume(layout)
                            }
                        }, "LECTAV")
                        try {
                            val template = context.assets.open(TEMPLATE)
                                .bufferedReader(Charsets.UTF_8).use { it.readText() }
                            val html = template
                                .replace("__LECTAV_TITLE_B64__", encode(documentTitle.take(100)))
                                .replace("__LECTAV_CONTENT_B64__", encode(body))
                            browser.loadDataWithBaseURL(
                                "file:///android_asset/math/", html,
                                "text/html", "UTF-8", null
                            )
                        } catch (e: Exception) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                }

                val printableWidth = PAGE_WIDTH - 2 * MARGIN
                val scale = printableWidth / VIEW_WIDTH
                val pageContentHeight = (PAGE_HEIGHT - 2 * MARGIN) / scale
                // The JS DOM paginator provides safe CSS-space block boundaries.
                // WebView may scale CSS px to actual canvas px on different tablets,
                // so convert using the WebView's measured physical width.
                val parsed = JSONObject(layoutJson)
                val cssWidth = parsed.getDouble("viewportWidth").toFloat()
                require(cssWidth in 250f..2000f) { "Amplada del document incorrecta." }
                val cssToCanvas = VIEW_WIDTH / cssWidth
                val rawPages = parsed.getJSONArray("pages")
                require(rawPages.length() in 1..MAX_PAGES) {
                    "El PDF supera el límit de $MAX_PAGES pàgines."
                }
                val pageRanges = (0 until rawPages.length()).map { index ->
                    val range = rawPages.getJSONArray(index)
                    require(range.length() == 2) { "Salt de pàgina incorrecte." }
                    val from = range.getDouble(0).toFloat() * cssToCanvas
                    val to = range.getDouble(1).toFloat() * cssToCanvas
                    require(from >= 0f && to > from &&
                        to - from <= pageContentHeight + 2f) {
                        "Una pàgina desborda l'espai imprimible."
                    }
                    if (index > 0) {
                        val previousEnd = rawPages.getJSONArray(index - 1)
                            .getDouble(1).toFloat() * cssToCanvas
                        require(kotlin.math.abs(from - previousEnd) < 2f) {
                            "Els salts de pàgina no són consecutius."
                        }
                    }
                    Pair(from, to)
                }
                val pageCount = pageRanges.size
                val height = ceil(pageRanges.last().second).toInt().coerceAtLeast(1123)
                // Lay out the complete HTML before vector/raster canvas capture.
                browser.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        VIEW_WIDTH, View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                browser.layout(0, 0, VIEW_WIDTH, height)

                val destination = withContext(Dispatchers.IO) {
                    File.createTempFile("lectav_", ".pdf", fileFolder)
                }
                try {
                    val pdf = PdfDocument()
                    try {
                        repeat(pageCount) { index ->
                            coroutineContext.ensureActive()
                            if (SystemClock.elapsedRealtime() - startTime > MAX_EXPORT_MS) {
                                error("La creació del PDF està trigant massa. " +
                                    "Divideix els exercicis en documents més curts.")
                            }
                            // WebView.draw() must remain on Android's UI thread.
                            // Yield between pages so input, redraw and cancellation
                            // can be processed, preventing long uninterrupted loops.
                            yield()
                            val page = pdf.startPage(
                                PdfDocument.PageInfo.Builder(
                                    PAGE_WIDTH, PAGE_HEIGHT, index + 1
                                ).create()
                            )
                            val canvas = page.canvas
                            canvas.drawColor(Color.WHITE)
                            canvas.save()
                            canvas.clipRect(
                                MARGIN, MARGIN, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN
                            )
                            canvas.translate(MARGIN, MARGIN)
                            canvas.scale(scale, scale)
                            val bounds = pageRanges[index]
                            // Clip at the chosen safe boundary, leaving blank
                            // bottom space rather than slicing a table or heading.
                            canvas.clipRect(
                                0f, 0f, VIEW_WIDTH.toFloat(),
                                (bounds.second - bounds.first).coerceAtMost(pageContentHeight)
                            )
                            canvas.translate(0f, -bounds.first)
                            browser.draw(canvas)
                            canvas.restore()
                            pdf.finishPage(page)
                            delay(24)
                        }
                        // Disk I/O and PDF verification were previously run on
                        // the UI thread, causing app-not-responding freezes.
                        withContext(Dispatchers.IO) {
                            destination.outputStream().buffered().use { pdf.writeTo(it) }
                            checkNotBlank(destination, pageCount)
                        }
                    } finally {
                        pdf.close()
                    }

                    FileProvider.getUriForFile(
                        context, "${context.packageName}.files", destination
                    )
                } catch (e: Exception) {
                    destination.delete()
                    throw e
                }
            } finally {
                // WebView ownership stays on the main thread. Release the
                // render resources even on error or coroutine cancellation.
                browser.removeJavascriptInterface("LECTAV")
                browser.stopLoading()
                browser.destroy()
            }
            } finally {
                exportMutex.unlock()
            }
        }

    /**
     * Reject empty PDF files or all-white first-page rendering.
     * This avoids claiming a successful conversion if the device's
     * WebView fails to draw off-screen content into PdfDocument.
     */
    private fun checkNotBlank(file: File, expectedPages: Int) {
        require(file.length() >= 500) { "No s'ha escrit un PDF vàlid." }
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd.use {
            PdfRenderer(it).use { renderer ->
                require(renderer.pageCount == expectedPages) {
                    "El PDF no conté totes les pàgines."
                }
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(210, 297, Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        var ink = 0
                        for (y in 0 until bitmap.height step 3) {
                            for (x in 0 until bitmap.width step 3) {
                                val pixel = bitmap.getPixel(x, y)
                                if (Color.red(pixel) < 210 ||
                                    Color.green(pixel) < 210 ||
                                    Color.blue(pixel) < 210
                                ) ink++
                            }
                        }
                        require(ink >= 8) {
                            "El visor de PDF ha generat una pàgina en blanc. " +
                            "Torna a provar-ho."
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }
}
