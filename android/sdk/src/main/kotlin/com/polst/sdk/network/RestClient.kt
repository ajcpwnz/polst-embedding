package com.polst.sdk.network

import com.polst.sdk.Environment
import com.polst.sdk.brand.Scope
import com.polst.sdk.core.deviceid.DeviceIdProvider
import com.polst.sdk.core.logging.NoOpPolstLogger
import com.polst.sdk.core.logging.PolstLogger
import com.polst.sdk.core.tokens.TokenStore
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.IOException
import java.net.URLEncoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class RestClient(
    private val transport: HttpTransport,
    private val environment: Environment,
    private val deviceIdProvider: DeviceIdProvider,
    private val tokenStore: TokenStore,
    private val logger: PolstLogger = NoOpPolstLogger,
    @PublishedApi internal val json: Json = defaultJson,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        private const val TAG = "RestClient"
        private const val MAX_RETRIES = 3
    }

    public suspend inline fun <reified Resp> get(
        path: String,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = false,
    ): Resp {
        val response = execute(
            method = HttpMethod.GET,
            path = path,
            query = query,
            bodyBytes = null,
            extraHeaders = emptyMap(),
            authenticated = authenticated,
        )
        return decode(response, serializer())
    }

    public suspend inline fun <reified Req, reified Resp> post(
        path: String,
        body: Req,
        headers: Map<String, String> = emptyMap(),
        authenticated: Boolean = false,
    ): Resp {
        val bodyBytes = json.encodeToString(serializer<Req>(), body).toByteArray(Charsets.UTF_8)
        val response = execute(
            method = HttpMethod.POST,
            path = path,
            query = emptyMap(),
            bodyBytes = bodyBytes,
            extraHeaders = headers,
            authenticated = authenticated,
        )
        return decode(response, serializer())
    }

    public suspend inline fun <reified Req, reified Resp> patch(
        path: String,
        body: Req,
        authenticated: Boolean = false,
    ): Resp {
        val bodyBytes = json.encodeToString(serializer<Req>(), body).toByteArray(Charsets.UTF_8)
        val response = execute(
            method = HttpMethod.PATCH,
            path = path,
            query = emptyMap(),
            bodyBytes = bodyBytes,
            extraHeaders = emptyMap(),
            authenticated = authenticated,
        )
        return decode(response, serializer())
    }

    public suspend fun delete(path: String, authenticated: Boolean = false) {
        execute(
            method = HttpMethod.DELETE,
            path = path,
            query = emptyMap(),
            bodyBytes = null,
            extraHeaders = emptyMap(),
            authenticated = authenticated,
        )
    }

    @PublishedApi
    internal fun <T> decode(response: HttpResponse, serializer: KSerializer<T>): T {
        val bodyBytes = response.body ?: ByteArray(0)
        val text = bodyBytes.toString(Charsets.UTF_8)
        return try {
            json.decodeFromString(serializer, text)
        } catch (t: Throwable) {
            throw PolstApiError.Decoding(t)
        }
    }

    @PublishedApi
    internal suspend fun execute(
        method: HttpMethod,
        path: String,
        query: Map<String, String?>,
        bodyBytes: ByteArray?,
        extraHeaders: Map<String, String>,
        authenticated: Boolean,
    ): HttpResponse {
        val url = buildUrl(path, query)
        val headers = buildHeaders(method, bodyBytes, extraHeaders, authenticated)
        val request = HttpRequest(url = url, method = method, headers = headers, body = bodyBytes)

        var attempt = 0
        var lastIo: IOException? = null
        while (attempt <= MAX_RETRIES) {
            val response: HttpResponse? = try {
                transport.send(request)
            } catch (io: IOException) {
                lastIo = io
                null
            }

            if (response == null) {
                if (attempt < MAX_RETRIES) {
                    logger.warn(TAG, "IOException on attempt ${attempt + 1}; retrying", lastIo)
                    delay(delayMs(attempt))
                    attempt++
                    continue
                }
                throw PolstApiError.Network(lastIo ?: IOException("Unknown network failure"))
            }

            val code = response.statusCode
            when {
                code in 200..299 -> return response
                code in 500..599 -> {
                    if (attempt < MAX_RETRIES) {
                        logger.warn(TAG, "5xx ($code) on attempt ${attempt + 1}; retrying")
                        delay(delayMs(attempt))
                        attempt++
                        continue
                    }
                    throw PolstApiError.ServerError(code, response.body)
                }
                else -> mapErrorResponse(response)
            }
        }
        throw PolstApiError.Network(lastIo ?: IOException("Retry loop exhausted"))
    }

    @PublishedApi
    internal fun buildUrl(path: String, query: Map<String, String?>): String {
        val base = environment.baseUrl + path
        val pairs = query.filterValues { it != null }
        if (pairs.isEmpty()) return base
        val qs = pairs.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        return "$base?$qs"
    }

    private suspend fun buildHeaders(
        method: HttpMethod,
        bodyBytes: ByteArray?,
        extraHeaders: Map<String, String>,
        authenticated: Boolean,
    ): Map<String, String> {
        val headers = linkedMapOf<String, String>()
        headers["User-Agent"] = userAgent()
        headers["X-Device-Id"] = deviceIdProvider.deviceId().toString()
        headers["Accept"] = "application/json"
        if ((method == HttpMethod.POST || method == HttpMethod.PATCH) && bodyBytes != null) {
            headers["Content-Type"] = "application/json; charset=utf-8"
        }
        if (authenticated) {
            val snapshot = tokenStore.read() ?: throw PolstApiError.AuthenticationMissing
            headers["Authorization"] = "Bearer ${snapshot.accessToken}"
        }
        for ((k, v) in extraHeaders) {
            headers[k] = v
        }
        // Suppress-unused suppression: clock reserved for future idempotency-key generation.
        @Suppress("UnusedPrivateProperty") val now = clock()
        return headers
    }

    private fun userAgent(): String {
        val libraryVersion = try {
            val clazz = Class.forName("com.polst.sdk.BuildConfig")
            val field = clazz.getField("LIBRARY_VERSION")
            (field.get(null) as? String) ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
        val androidVersion = try {
            val clazz = Class.forName("android.os.Build\$VERSION")
            val field = clazz.getField("RELEASE")
            (field.get(null) as? String) ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
        val deviceModel = try {
            val clazz = Class.forName("android.os.Build")
            val field = clazz.getField("MODEL")
            (field.get(null) as? String) ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
        return "PolstSDK-Android/$libraryVersion ($androidVersion; $deviceModel)"
    }

    private fun delayMs(attempt: Int): Long = when (attempt) {
        0 -> 250L
        1 -> 1000L
        2 -> 4000L
        else -> 4000L
    }

    private fun mapErrorResponse(response: HttpResponse): Nothing {
        val code = response.statusCode
        val bodyBytes = response.body
        val bodyText: String? = bodyBytes?.toString(Charsets.UTF_8)
        val envelope: ErrorEnvelopeDto? = bodyText?.let { text ->
            try {
                json.decodeFromString(ErrorEnvelopeDto.serializer(), text)
            } catch (_: Throwable) {
                null
            }
        }
        val error = envelope?.error
        val serverCode = error?.code
        val message = error?.message

        when (code) {
            401 -> throw PolstApiError.AuthenticationExpired
            403 -> {
                val scopeWire = error?.scope
                if (serverCode == "SCOPE_REQUIRED" && scopeWire != null) {
                    val scope = Scope.entries.firstOrNull { it.wireValue == scopeWire }
                    if (scope != null) throw PolstApiError.AuthorizationInsufficient(scope)
                }
                throw PolstApiError.HttpStatus(403, bodyBytes)
            }
            404 -> {
                // API error shape: {"error":{"code":"NOT_FOUND","message":"<resource> not found.","details":null}}
                // Best-effort: resource = lowercased first word of message, id taken from the request URL.
                val resource = message
                    ?.substringBefore(' ')
                    ?.lowercase()
                    ?.ifBlank { "resource" } ?: "resource"
                val id = response.headers["X-Request-Id"]?.firstOrNull() ?: ""
                throw PolstApiError.NotFound(resource = resource, id = id)
            }
            400, 422 -> {
                val fields = error?.fields
                if (!fields.isNullOrEmpty()) {
                    throw PolstApiError.Validation(fields)
                }
                throw PolstApiError.HttpStatus(code, bodyBytes)
            }
            429 -> throw PolstApiError.RateLimited(parseRetryAfter(response.headers))
            else -> throw PolstApiError.HttpStatus(code, bodyBytes)
        }
    }

    private fun parseRetryAfter(headers: Map<String, List<String>>): Duration? {
        val value = headers.entries
            .firstOrNull { it.key.equals("Retry-After", ignoreCase = true) }
            ?.value
            ?.firstOrNull()
            ?: return null
        val seconds = value.trim().toLongOrNull() ?: return null
        return seconds.seconds
    }
}

/**
 * Error responses from the server follow the shape
 * `{"error": {"code": "...", "message": "...", "fields": {...}?, "scope": "..."?}}`.
 * Matches iOS `ErrorEnvelopeDTO`.
 */
@kotlinx.serialization.Serializable
internal data class ErrorEnvelopeDto(val error: ErrorBody) {
    @kotlinx.serialization.Serializable
    internal data class ErrorBody(
        val code: String? = null,
        val message: String? = null,
        val fields: Map<String, String>? = null,
        val scope: String? = null,
    )
}
