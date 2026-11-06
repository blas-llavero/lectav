/*
 * LECTAV — Keystore-encrypted ChatGPT OAuth credentials.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject
import java.util.UUID

internal data class ChatGPTSession(
    val subject: String,
    val email: String,
    val clientId: String,
    val idToken: String,
    val accessToken: String,
    val refreshToken: String,
    val scopes: Set<String>,
    val expiresAt: Long
) {
    val canGenerate: Boolean
        get() = "chatgpt.tokens.use.direct" in scopes &&
                "resource.invoke" in scopes
}

internal class SecureChatGPTStore(private val context: Context) {
    private val file get() = File(context.filesDir, "chatgpt-credentials.enc")
    private val alias = "lectav_chatgpt_oauth_v1"

    fun hostId(): String {
        val prefs = context.getSharedPreferences("lectav_chatgpt_host", Context.MODE_PRIVATE)
        val existing = prefs.getString("id", null)
        if (existing != null && existing.startsWith("urn:uuid:")) return existing
        val created = "urn:uuid:" + UUID.randomUUID().toString()
        check(prefs.edit().putString("id", created).commit())
        return created
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun save(session: ChatGPTSession) {
        val raw = JSONObject().apply {
            put("sub", session.subject)
            put("email", session.email)
            put("client", session.clientId)
            put("id_token", session.idToken)
            put("access", session.accessToken)
            put("refresh", session.refreshToken)
            put("scopes", session.scopes.sorted().joinToString(" "))
            put("expires", session.expiresAt)
        }.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(raw)
        val tmp = File(context.filesDir, "chatgpt-credentials.tmp")
        tmp.outputStream().use {
            it.write(cipher.iv.size)
            it.write(cipher.iv)
            it.write(ciphertext)
            it.fd.sync()
        }
        check(tmp.renameTo(file)) { "Could not save encrypted credentials" }
    }

    fun load(): ChatGPTSession? {
        if (!file.exists()) return null
        val bytes = file.readBytes()
        require(bytes.size in 40..500_000) { "Invalid encrypted credential data" }
        val n = bytes[0].toInt() and 255
        require(n == 12 && bytes.size > n + 17)
        val iv = bytes.copyOfRange(1, n + 1)
        val ciphertext = bytes.copyOfRange(n + 1, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        val obj = JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        return ChatGPTSession(
            subject = obj.getString("sub"),
            email = obj.optString("email"),
            clientId = obj.getString("client"),
            idToken = obj.getString("id_token"),
            accessToken = obj.getString("access"),
            refreshToken = obj.optString("refresh"),
            scopes = obj.optString("scopes").split(" ").filter { it.isNotBlank() }.toSet(),
            expiresAt = obj.getLong("expires")
        )
    }

    fun clear() {
        file.delete()
        File(context.filesDir, "chatgpt-credentials.tmp").delete()
    }
}
