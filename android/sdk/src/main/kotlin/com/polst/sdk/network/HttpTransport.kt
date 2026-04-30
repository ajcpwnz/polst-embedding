package com.polst.sdk.network

internal enum class HttpMethod {
    GET,
    POST,
    PATCH,
    DELETE,
}

internal data class HttpRequest(
    val url: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
)

internal data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: ByteArray?,
)

internal interface HttpTransport {
    suspend fun send(request: HttpRequest): HttpResponse
}
