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
import java.io.File
import java.io.IOException
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
    private const val MAX_PAGES = 40

    private fun encode(s: String): String = Base64.encodeToString(
        s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
    )

    private class RenderReady(private val onReady: (String, Int) -> Unit) {
        private val main = Handler(Looper.getMainLooper())

        @JavascriptInterface
        fun complete(error: String, height: Int) {
            main.post { onReady(error.take(300), height) }
        }
    }

    suspend fun save(context: Context, documentTitle: String, body: String): Uri =
        withContext(Dispatchers.Main) {
            require(body.isNotBlank()) { "No hi ha contingut per desar." }
            require(body.length <= 50_000) { "L'esborrany supera el límit d'aquest prototip." }

            // Android 5.0+: allow software drawing of the entire document,
            // not only the currently visible WebView viewport.
            WebView.enableSlowWholeDocumentDraw()
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

                val height = withTimeout(60_000L) {
                    suspendCancellableCoroutine<Int> { continuation ->
                        browser.addJavascriptInterface(RenderReady { error, measuredHeight ->
                            if (!continuation.isActive) return@RenderReady
                            if (error.isNotBlank()) {
                                continuation.resumeWithException(IOException(error))
                            } else if (measuredHeight < 40) {
                                continuation.resumeWithException(
                                    IOException("El document ha quedat buit.")
                                )
                            } else {
                                continuation.resume(measuredHeight)
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
                val pageCount = ceil(height / pageContentHeight).toInt().coerceAtLeast(1)
                require(pageCount <= MAX_PAGES) {
                    "Aquest document és massa llarg ($pageCount pàgines). " +
                    "Divideix-lo en sessions o unitats més petites."
                }
                // Lay out the complete HTML before vector/raster canvas capture.
                browser.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        VIEW_WIDTH, View.MeasureSpec.EXACTLY
                    ),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                browser.layout(0, 0, VIEW_WIDTH, height)

                val destination = File.createTempFile("lectav_", ".pdf", fileFolder)
                try {
                    val pdf = PdfDocument()
                    try {
                        repeat(pageCount) { index ->
                            coroutineContext.ensureActive()
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
                            canvas.translate(0f, -index * pageContentHeight)
                            browser.draw(canvas)
                            canvas.restore()
                            pdf.finishPage(page)
                        }
                        destination.outputStream().use { pdf.writeTo(it) }
                    } finally {
                        pdf.close()
                    }

                    checkNotBlank(destination, pageCount)
                    FileProvider.getUriForFile(
                        context, "${context.packageName}.files", destination
                    )
                } catch (e: Exception) {
                    destination.delete()
                    throw e
                }
            } finally {
                browser.destroy()
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
