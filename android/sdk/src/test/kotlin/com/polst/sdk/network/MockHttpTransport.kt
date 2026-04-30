package com.polst.sdk.network

/**
 * Internal, unit-test-only mock transport.
 *
 * Visibility decision: `HttpTransport` is kept internal to avoid leaking a transport abstraction
 * into the SDK's public API. A cross-module mock for host integrators (in `:sdk-test`) is
 * deferred to a later phase, once the public seam is deliberately designed.
 */
internal class MockHttpTransport(
    private val handler: suspend (HttpRequest) -> HttpResponse,
) : HttpTransport {
    override suspend fun send(request: HttpRequest): HttpResponse = handler(request)
}

internal class MockHttpTransportBuilder {
    private data class Route(
        val method: HttpMethod,
        val urlSubstring: String,
        val respond: suspend (HttpRequest) -> HttpResponse,
    )

    private val routes: MutableList<Route> = mutableListOf()
    private var fallback: (suspend (HttpRequest) -> HttpResponse)? = null

    fun on(
        method: HttpMethod,
        urlSubstring: String,
        respond: suspend (HttpRequest) -> HttpResponse,
    ) {
        routes += Route(method, urlSubstring, respond)
    }

    fun fallback(respond: suspend (HttpRequest) -> HttpResponse) {
        fallback = respond
    }

    fun build(): MockHttpTransport = MockHttpTransport { request ->
        val route = routes.firstOrNull { r ->
            r.method == request.method && request.url.contains(r.urlSubstring)
        }
        when {
            route != null -> route.respond(request)
            fallback != null -> fallback!!.invoke(request)
            else -> error("No mock route matches ${request.method} ${request.url}")
        }
    }
}

internal fun mockHttpTransport(block: MockHttpTransportBuilder.() -> Unit): MockHttpTransport =
    MockHttpTransportBuilder().apply(block).build()
