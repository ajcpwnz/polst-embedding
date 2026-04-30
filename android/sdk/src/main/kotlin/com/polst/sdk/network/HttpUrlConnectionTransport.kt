package com.polst.sdk.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal class HttpUrlConnectionTransport(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 30_000,
) : HttpTransport {

    override suspend fun send(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
            requestMethod = when (request.method) {
                HttpMethod.GET -> "GET"
                HttpMethod.POST -> "POST"
                HttpMethod.PATCH -> "PATCH"
                HttpMethod.DELETE -> "DELETE"
            }
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            useCaches = false
            instanceFollowRedirects = true
            for ((name, value) in request.headers) {
                setRequestProperty(name, value)
            }
            if (request.method == HttpMethod.POST || request.method == HttpMethod.PATCH) {
                doOutput = true
            }
        }

        try {
            val body = request.body
            if ((request.method == HttpMethod.POST || request.method == HttpMethod.PATCH) && body != null) {
                connection.outputStream.use { out -> out.write(body) }
            }

            val statusCode = connection.responseCode
            val headers: Map<String, List<String>> = connection.headerFields
                .filterKeys { it != null }
                .mapKeys { it.key!! }

            val stream: InputStream? = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = stream?.use { it.readBytes() }

            HttpResponse(statusCode = statusCode, headers = headers, body = responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
