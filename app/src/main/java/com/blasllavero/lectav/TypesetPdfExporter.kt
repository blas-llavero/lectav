/*
 * LECTAV — offline, typeset Markdown/LaTeX PDF export.
 * Copyright (C) 2026 Blas Llavero
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Converts user-approved draft Markdown with math formulas into a real A4 PDF.
 * KaTeX, markdown-it, CSS and fonts are **packaged in the APK**. No renderer
 * website is loaded and no document data is sent to another service.
 *
 * NOTE: The off-screen Android WebView print path requires on-device testing
 * beyond the automated Android compilation check.
 */
internal object TypesetPdfExporter {
    private const val HTML_TEMPLATE = "math/print-template.html"
    private const val PAGE_TIMEOUT_MS = 120_000L

    private fun encoded(text: String): String = Base64.encodeToString(
        text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
    )

    private class ReadyBridge(private val callback: (String) -> Unit) {
        private val main = Handler(Looper.getMainLooper())
        @JavascriptInterface
        fun complete(error: String) {
            main.post { callback(error.take(300)) }
        }
    }

    suspend fun save(context: Context, documentTitle: String, content: String): Uri =
        withContext(Dispatchers.Main) {
            require(content.isNotBlank()) { "No hi ha contingut per desar." }
            require(content.length <= 50_000) {
                "El document és massa llarg per a aquest prototip."
            }
            val folder = File(context.filesDir, "generated").apply {
                check(isDirectory || mkdirs()) { "No es pot crear la carpeta local." }
            }
            val file = File.createTempFile("lectav_", ".pdf", folder)
            try {
                withTimeout(PAGE_TIMEOUT_MS) {
                    suspendCancellableCoroutine<Uri> { continuation ->
                        var web: WebView? = null
                        var printAdapter: PrintDocumentAdapter? = null
                        var output: ParcelFileDescriptor? = null
                        var terminated = false

                        fun cleanup() {
                            runCatching { output?.close() }
                            output = null
                            runCatching { printAdapter?.onFinish() }
                            printAdapter = null
                            runCatching { web?.destroy() }
                            web = null
                        }

                        fun fail(message: String) {
                            if (terminated) return
                            terminated = true
                            cleanup()
                            file.delete()
                            if (continuation.isActive) {
                                continuation.resumeWithException(IOException(message))
                            }
                        }

                        fun finish() {
                            if (terminated) return
                            terminated = true
                            cleanup()
                            val headerValid = runCatching {
                                file.inputStream().use { stream ->
                                    val bytes = ByteArray(4)
                                    stream.read(bytes) == 4 &&
                                        String(bytes, Charsets.US_ASCII) == "%PDF"
                                }
                            }.getOrDefault(false)
                            if (!headerValid || file.length() < 500L) {
                                file.delete()
                                if (continuation.isActive) {
                                    continuation.resumeWithException(
                                        IOException("No s'ha pogut crear un PDF vàlid.")
                                    )
                                }
                                return
                            }
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.files", file
                            )
                            if (continuation.isActive) continuation.resume(uri)
                        }

                        continuation.invokeOnCancellation {
                            Handler(Looper.getMainLooper()).post {
                                if (!terminated) {
                                    terminated = true
                                    cleanup()
                                    file.delete()
                                }
                            }
                        }

                        try {
                            // All WebView and PrintDocumentAdapter calls are on the main thread.
                            val browser = WebView(context)
                            web = browser
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
                                View.MeasureSpec.makeMeasureSpec(794, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(1123, View.MeasureSpec.EXACTLY)
                            )
                            browser.layout(0, 0, 794, 1123)
                            browser.addJavascriptInterface(ReadyBridge { error ->
                                if (terminated) return@ReadyBridge
                                if (error.isNotBlank()) {
                                    fail(error)
                                    return@ReadyBridge
                                }
                                try {
                                    val adapter = browser.createPrintDocumentAdapter(
                                        documentTitle.take(90).ifBlank { "LECTAV" }
                                    )
                                    printAdapter = adapter
                                    val attributes = PrintAttributes.Builder()
                                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                        .setResolution(
                                            PrintAttributes.Resolution("pdf", "PDF", 300, 300)
                                        )
                                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                                        .build()
                                    adapter.onStart()
                                    adapter.onLayout(
                                        null, attributes, CancellationSignal(),
                                        object : PrintDocumentAdapter.LayoutResultCallback() {
                                            override fun onLayoutFinished(
                                                info: PrintDocumentInfo, changed: Boolean
                                            ) {
                                                if (terminated) return
                                                try {
                                                    val descriptor = ParcelFileDescriptor.open(
                                                        file,
                                                        ParcelFileDescriptor.MODE_CREATE or
                                                            ParcelFileDescriptor.MODE_TRUNCATE or
                                                            ParcelFileDescriptor.MODE_READ_WRITE
                                                    )
                                                    output = descriptor
                                                    adapter.onWrite(
                                                        arrayOf(PageRange.ALL_PAGES),
                                                        descriptor,
                                                        CancellationSignal(),
                                                        object : PrintDocumentAdapter.WriteResultCallback() {
                                                            override fun onWriteFinished(
                                                                pages: Array<out PageRange>
                                                            ) {
                                                                finish()
                                                            }
                                                            override fun onWriteFailed(
                                                                error: CharSequence?
                                                            ) {
                                                                fail(error?.toString()
                                                                    ?: "No s'ha pogut escriure el PDF.")
                                                            }
                                                            override fun onWriteCancelled() {
                                                                fail("La creació del PDF s'ha cancel·lat.")
                                                            }
                                                        }
                                                    )
                                                } catch (e: Exception) {
                                                    fail(e.localizedMessage
                                                        ?: "No s'ha pogut iniciar el PDF.")
                                                }
                                            }
                                            override fun onLayoutFailed(error: CharSequence?) {
                                                fail(error?.toString()
                                                    ?: "No s'ha pogut preparar el format del PDF.")
                                            }
                                            override fun onLayoutCancelled() {
                                                fail("La preparació del PDF s'ha cancel·lat.")
                                            }
                                        },
                                        Bundle()
                                    )
                                } catch (e: Exception) {
                                    fail(e.localizedMessage
                                        ?: "No es pot inicialitzar el motor de PDF.")
                                }
                            }, "LECTAV")

                            val template = context.assets.open(HTML_TEMPLATE)
                                .bufferedReader(Charsets.UTF_8).use { it.readText() }
                            val document = template
                                .replace("__LECTAV_TITLE_B64__", encoded(documentTitle.take(100)))
                                .replace("__LECTAV_CONTENT_B64__", encoded(content))
                            browser.loadDataWithBaseURL(
                                "file:///android_asset/math/", document, "text/html", "UTF-8", null
                            )
                        } catch (e: Exception) {
                            fail(e.localizedMessage
                                ?: "No s'han pogut carregar les fonts matemàtiques locals.")
                        }
                    }
                }
            } catch (e: Exception) {
                file.delete()
                throw e
            }
        }
}
