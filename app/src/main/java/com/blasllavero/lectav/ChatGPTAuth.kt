/*
 * LECTAV — public-client OAuth 2.0 + PKCE, OpenID Connect validation.
 * Copyright (C) 2026 Blas Llavero. SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.blasllavero.lectav

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.security.spec.RSAPublicKeySpec
import java.util.Locale

internal data class ChatGPTAuthState(
    val connected: Boolean = false,
    val email: String = "",
    val inferenceAllowed: Boolean = false,
    val busy: Boolean = false,
    val message: String = ""
)

internal data class ChatGPTModel(val slug: String, val name: String)

// This preview supports one locally active account. Multi-account separation
// and verified child profiles must be implemented before school deployment.
internal object ChatGPTAuth {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionLock = Mutex()
    private lateinit var store: SecureChatGPTStore
    private var initialized = false
    private var current: ChatGPTSession? = null
    private val mutableState = MutableStateFlow(ChatGPTAuthState())
    val state = mutableState.asStateFlow()

    fun initialize(context: Context) {
        if (initialized) return
        store = SecureChatGPTStore(context.applicationContext)
        current = runCatching { store.load() }.getOrNull()
        initialized = true
        publish()
    }

    private fun publish(busy: Boolean = false, message: String = "") {
        val user = current
        mutableState.value = ChatGPTAuthState(
            connected = user != null,
            email = user?.email.orEmpty(),
            inferenceAllowed = user?.canGenerate == true,
            busy = busy,
            message = message
        )
    }

    fun signIn(context: Context) {
        initialize(context)
        if (state.value.busy) return
        publish(busy = true, message = "Obre el navegador i autoritza LECTAV.")
        scope.launch {
            val result = runCatching { performSignIn(context) }
            result.onSuccess {
                publish(message = if (it.canGenerate)
                    "ChatGPT connectat. Pots utilitzar els models disponibles al compte."
                else "Compte connectat, però no s'ha autoritzat la generació amb el pla.")
            }.onFailure {
                publish(message = "No s'ha completat la connexió. " +
                    "Comprova el navegador, la connexió i els permisos.")
            }
        }
    }

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun fixedEquals(first: String, second: String): Boolean =
        MessageDigest.isEqual(first.toByteArray(Charsets.UTF_8), second.toByteArray(Charsets.UTF_8))

    private data class Callback(
        val code: String,
        val clientId: String,
        val error: String = ""
    )

    private suspend fun performSignIn(context: Context): ChatGPTSession {
        val previous = current
        val isNew = previous == null
        val initialClientId = previous?.clientId ?: "dynamic_agent_client"
        val nonce = randomToken()
        val state = randomToken()
        val verifier = randomToken()
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(Charsets.US_ASCII)),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        val socket = withContext(Dispatchers.IO) {
            ServerSocket(0, 8, InetAddress.getByName("127.0.0.1")).also {
                it.soTimeout = 120_000
            }
        }
        socket.use { server ->
            val callbackUrl = "http://127.0.0.1:${server.localPort}/auth/callback"
            val builder = Uri.parse("https://auth.openai.com/api/accounts/authorize")
                .buildUpon()
                .appendQueryParameter("client_id", initialClientId)
                .appendQueryParameter("ext_agent_host_id", store.hostId())
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", callbackUrl)
                .appendQueryParameter(
                    "scope",
                    "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct"
                )
                .appendQueryParameter("resource", "https://api.openai.com/v1")
                .appendQueryParameter("state", state)
                .appendQueryParameter("nonce", nonce)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("code_challenge", challenge)
            if (isNew) builder.appendQueryParameter("agent_name_hint", "LECTAV")
            else previous?.idToken?.let {
                builder.appendQueryParameter("id_token_hint", it)
            }
            val authorization = builder.build()
            val intent = Intent(Intent.ACTION_VIEW, authorization).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val callback = withContext(Dispatchers.IO) {
                receiveCallback(server, state, initialClientId, isNew)
            }
            require(callback.error.isEmpty()) { "The user declined authorization" }
            val token = withContext(Dispatchers.IO) {
                val reply = postForm("https://auth.openai.com/api/accounts/oauth/token", mapOf(
                    "grant_type" to "authorization_code",
                    "client_id" to callback.clientId,
                    "code" to callback.code,
                    "code_verifier" to verifier,
                    "redirect_uri" to callbackUrl,
                    "resource" to "https://api.openai.com/v1"
                ))
                val issued = reply.getString("id_token")
                val info = verifyIdToken(issued, callback.clientId, nonce)
                if (previous != null) {
                    require(info.first == previous.subject) {
                        "Account mismatch during reauthorization"
                    }
                }
                val scopes = reply.optString("scope")
                    .split(" ").filter { it.isNotBlank() }.toSet()
                val session = ChatGPTSession(
                    subject = info.first,
                    email = info.second,
                    clientId = callback.clientId,
                    idToken = issued,
                    accessToken = reply.getString("access_token"),
                    refreshToken = reply.optString("refresh_token"),
                    scopes = scopes,
                    expiresAt = System.currentTimeMillis() / 1000 +
                        reply.optLong("expires_in", 3600)
                )
                store.save(session)
                session
            }
            sessionLock.withLock { current = token }
            return token
        }
    }

    private fun receiveCallback(
        server: ServerSocket,
        expectedState: String,
        expectedClient: String,
        isNew: Boolean
    ): Callback {
        val until = System.currentTimeMillis() + 120_000
        while (System.currentTimeMillis() < until) {
            val client = try {
                server.accept()
            } catch (_: SocketTimeoutException) {
                break
            }
            val callback: Callback? = client.use { incoming ->
                incoming.soTimeout = 5_000
                val line = runCatching {
                    incoming.getInputStream().bufferedReader(Charsets.US_ASCII).readLine()
                }.getOrNull().orEmpty()
                val target = line.split(" ").getOrNull(1).orEmpty()
                if (!line.startsWith("GET ") ||
                    !target.startsWith("/auth/callback?") ||
                    target.length > 8192
                ) {
                    reply(incoming, 404, "Aquest enllaç no és vàlid.")
                    null
                } else {
                    val uri = runCatching {
                        Uri.parse("http://127.0.0.1${target}")
                    }.getOrNull()
                    val receivedState = uri?.getQueryParameter("state").orEmpty()
                    if (!fixedEquals(expectedState, receivedState)) {
                        reply(incoming, 400, "La verificació de la sessió ha fallat.")
                        null
                    } else {
                        val failure = uri?.getQueryParameter("error").orEmpty()
                        if (failure.isNotBlank()) {
                            reply(incoming, 200, "Autorització cancel·lada. Torna a LECTAV.")
                            Callback("", expectedClient, failure)
                        } else {
                            val code = uri?.getQueryParameter("code").orEmpty()
                            val issuedClient = uri?.getQueryParameter("client_id").orEmpty()
                            val actualClient = if (isNew) issuedClient else expectedClient
                            if (code.isBlank() || actualClient.isBlank() ||
                                actualClient == "dynamic_agent_client" ||
                                (!isNew && issuedClient.isNotBlank() &&
                                    issuedClient != expectedClient)
                            ) {
                                reply(incoming, 400, "No s'ha completat l'autorització.")
                                null
                            } else {
                                reply(incoming, 200, "Autorització rebuda. Torna a LECTAV.")
                                Callback(code, actualClient)
                            }
                        }
                    }
                }
            }
            if (callback != null) return callback
        }
        error("La connexió ha caducat. Torna-ho a provar des de LECTAV.")
    }

    private fun reply(client: Socket, status: Int, message: String) {
        val body = ("<!doctype html><html lang='ca'><meta charset='utf-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>LECTAV</title><body><h2>LECTAV</h2><p>" +
            message + "</p></body></html>").toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $status ${if(status==200) "OK" else "Error"}\r\n" +
            "Content-Type: text/html; charset=utf-8\r\n" +
            "Content-Security-Policy: default-src 'none'\r\n" +
            "Cache-Control: no-store\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
        runCatching {
            client.getOutputStream().write(headers.toByteArray(Charsets.US_ASCII))
            client.getOutputStream().write(body)
            client.getOutputStream().flush()
        }
    }

    private fun postForm(url: String, fields: Map<String, String>): JSONObject {
        val payload = fields.entries.joinToString("&") { (k,v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            instanceFollowRedirects = false
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            require(connection.responseCode == 200) {
                "ChatGPT OAuth HTTP ${connection.responseCode}"
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = 12_000
            readTimeout = 15_000
        }
        try {
            require(connection.responseCode == 200) {
                "Unable to load identity verification keys"
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun decodeJwtSegment(text: String): ByteArray =
        Base64.decode(text, Base64.URL_SAFE or Base64.NO_WRAP)

    private fun verifyIdToken(jwt: String, clientId: String, nonce: String): Pair<String, String> {
        val segments = jwt.split(".")
        require(segments.size == 3) { "Invalid signed ID token" }
        val header = JSONObject(String(decodeJwtSegment(segments[0]), Charsets.UTF_8))
        val payload = JSONObject(String(decodeJwtSegment(segments[1]), Charsets.UTF_8))
        val algorithm = header.optString("alg")
        require(algorithm == "RS256" || algorithm == "PS256")
        val kid = header.getString("kid")
        val discovery = fetchJson("https://auth.openai.com/.well-known/openid-configuration")
        val jwksUrl = discovery.getString("jwks_uri")
        require(URL(jwksUrl).protocol == "https" && URL(jwksUrl).host == "auth.openai.com") {
            "Unexpected identity key host"
        }
        val keys = fetchJson(jwksUrl).getJSONArray("keys")
        val jwk = (0 until keys.length()).map { keys.getJSONObject(it) }
            .firstOrNull {
                it.optString("kid") == kid && it.optString("kty") == "RSA" &&
                    (it.optString("alg").isBlank() || it.optString("alg") == algorithm)
            } ?: error("No matching identity verification key")
        val modulus = BigInteger(1, decodeJwtSegment(jwk.getString("n")))
        val exponent = BigInteger(1, decodeJwtSegment(jwk.getString("e")))
        val key = KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(modulus, exponent))
        val verifier = if (algorithm == "RS256") Signature.getInstance("SHA256withRSA")
            else Signature.getInstance("RSASSA-PSS").apply {
                setParameter(PSSParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, 32, 1))
            }
        verifier.initVerify(key)
        verifier.update("${segments[0]}.${segments[1]}".toByteArray(Charsets.US_ASCII))
        require(verifier.verify(decodeJwtSegment(segments[2]))) {
            "Untrusted ID token"
        }
        require(payload.optString("iss") == "https://auth.openai.com")
        val audience = payload.opt("aud")
        val matchesAudience = when (audience) {
            is String -> audience == clientId
            is JSONArray -> (0 until audience.length()).any {
                audience.optString(it) == clientId
            }
            else -> false
        }
        require(matchesAudience) { "Wrong token audience" }
        val now = System.currentTimeMillis() / 1000
        require(payload.optLong("exp") > now &&
            payload.optLong("iat") <= now + 120 &&
            payload.optLong("iat") > 0)
        require(fixedEquals(payload.optString("nonce"), nonce)) { "Wrong OAuth nonce" }
        val subject = payload.getString("sub")
        require(subject.isNotBlank())
        return subject to payload.optString("email")
    }

    suspend fun bearerToken(): String = withContext(Dispatchers.IO) {
        sessionLock.withLock {
            val session = current ?: error("Inicia sessió amb ChatGPT.")
            require(session.canGenerate) { "El compte no ha autoritzat l'ús del seu pla." }
            if (session.expiresAt > System.currentTimeMillis() / 1000 + 120) {
                return@withLock session.accessToken
            }
            require(session.refreshToken.isNotBlank()) {
                "Cal tornar a iniciar sessió amb ChatGPT."
            }
            val response = postForm("https://auth.openai.com/api/accounts/oauth/token",
                mapOf(
                    "grant_type" to "refresh_token",
                    "client_id" to session.clientId,
                    "refresh_token" to session.refreshToken,
                    "resource" to "https://api.openai.com/v1"
                ))
            val renewed = session.copy(
                accessToken = response.getString("access_token"),
                refreshToken = response.optString("refresh_token", session.refreshToken),
                expiresAt = System.currentTimeMillis() / 1000 +
                    response.optLong("expires_in", 3600),
                scopes = response.optString("scope")
                    .takeIf { it.isNotBlank() }
                    ?.split(" ")?.toSet() ?: session.scopes
            )
            store.save(renewed)
            current = renewed
            publish()
            renewed.accessToken
        }
    }

    fun signOut() {
        if (state.value.busy) return
        publish(busy = true, message = "Desconnectant…")
        scope.launch {
            val currentSession = current
            val revoked = withContext(Dispatchers.IO) {
                runCatching {
                    if (!currentSession?.refreshToken.isNullOrBlank()) {
                        val discovery = fetchJson(
                            "https://auth.openai.com/.well-known/openid-configuration"
                        )
                        val revocation = discovery.getString("revocation_endpoint")
                        require(URL(revocation).protocol == "https" &&
                            URL(revocation).host == "auth.openai.com")
                        postFormAllowEmpty(revocation, mapOf(
                            "token" to currentSession!!.refreshToken,
                            "token_type_hint" to "refresh_token",
                            "client_id" to currentSession.clientId
                        ))
                    }
                }.isSuccess
            }
            sessionLock.withLock {
                current = null
                store.clear()
            }
            publish(message = if (revoked) "Sessió local tancada."
                else "Sessió local tancada, però no s'ha confirmat la revocació remota. " +
                "Revisa l'accés de LECTAV a la configuració de ChatGPT.")
        }
    }

    private fun postFormAllowEmpty(url: String, fields: Map<String,String>) {
        val payload = fields.entries.joinToString("&") { (k,v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            require(connection.responseCode == 200)
        } finally {
            connection.disconnect()
        }
    }
}
