package com.polst.example

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process HTTP server returning canned Polst SDK responses on
 * `http://127.0.0.1:8765/api/rest/v1/...`. Lives for the app process lifetime
 * so the example screens can exercise the full SDK pipeline (REST, offline
 * cache, vote replay) without needing a real backend.
 *
 * Demo seed:
 *   - Polst short id `demo-fruit`: "What's your favorite fruit?" with 3 options
 *   - Polst short id `demo-coffee`: "Coffee or tea?" with 2 options
 * Tallies update in-memory on each vote so the user sees the bar move.
 */
internal object MockBackend {

    const val PORT: Int = 8765
    const val BASE_URL: String = "http://127.0.0.1:$PORT/api/rest/v1"
    private const val TAG = "PolstMockBackend"

    private var server: PolstHttpServer? = null

    private val tallies: MutableMap<String, MutableMap<String, Long>> =
        ConcurrentHashMap<String, MutableMap<String, Long>>().apply {
            put(
                "demo-fruit",
                ConcurrentHashMap<String, Long>().apply {
                    put("opt-apple", 42L); put("opt-banana", 17L); put("opt-cherry", 9L)
                },
            )
            put(
                "demo-coffee",
                ConcurrentHashMap<String, Long>().apply {
                    put("opt-coffee", 128L); put("opt-tea", 96L)
                },
            )
        }

    fun start() {
        if (server != null) return
        try {
            val srv = PolstHttpServer(PORT, tallies)
            srv.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
            server = srv
            Log.i(TAG, "MockBackend listening on $BASE_URL")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start MockBackend on port $PORT: ${e.message}")
        }
    }

    fun stop() {
        server?.stop()
        server = null
    }

    private class PolstHttpServer(
        port: Int,
        private val tallies: MutableMap<String, MutableMap<String, Long>>,
    ) : NanoHTTPD("127.0.0.1", port) {

        override fun serve(session: IHTTPSession): Response {
            val path = session.uri
            if (!path.startsWith("/api/rest/v1/polsts/")) {
                return json(Response.Status.NOT_FOUND, """{"resource":"unknown","id":"$path"}""")
            }
            val parts = path.removePrefix("/api/rest/v1/polsts/").split("/")
            val shortId = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                ?: return json(Response.Status.NOT_FOUND, """{"resource":"polst","id":""}""")

            return when {
                session.method == Method.GET && parts.size == 1 -> {
                    val polstJson = polstJson(shortId)
                    if (polstJson != null) json(Response.Status.OK, polstJson)
                    else json(Response.Status.NOT_FOUND, """{"resource":"polst","id":"$shortId"}""")
                }

                session.method == Method.POST && parts.size == 2 && parts[1] == "votes" -> {
                    val files = HashMap<String, String>()
                    runCatching { session.parseBody(files) }
                    val bodyStr = files["postData"] ?: ""
                    val optionId = OPTION_RE.find(bodyStr)?.groupValues?.getOrNull(1)
                        ?: return json(
                            Response.Status.lookup(422),
                            """{"field_errors":{"option_id":"required"}}""",
                        )
                    val polstTallies = tallies[shortId]
                        ?: return json(Response.Status.NOT_FOUND, """{"resource":"polst","id":"$shortId"}""")
                    polstTallies.merge(optionId, 1L) { a, b -> a + b }
                    val tallyJson = polstTallies.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
                    json(
                        Response.Status.OK,
                        """{"vote_id":"v_${System.currentTimeMillis()}","tallies":{$tallyJson}}""",
                    )
                }

                else -> json(Response.Status.NOT_FOUND, """{"resource":"unknown","id":"$path"}""")
            }
        }

        private fun polstJson(shortId: String): String? = when (shortId) {
            "demo-fruit" -> seededPolstJson(
                shortId = "demo-fruit",
                question = "What's your favorite fruit?",
                options = listOf("opt-apple" to "Apple", "opt-banana" to "Banana", "opt-cherry" to "Cherry"),
                tallies = tallies["demo-fruit"] ?: emptyMap(),
            )
            "demo-coffee" -> seededPolstJson(
                shortId = "demo-coffee",
                question = "Coffee or tea?",
                options = listOf("opt-coffee" to "Coffee", "opt-tea" to "Tea"),
                tallies = tallies["demo-coffee"] ?: emptyMap(),
            )
            else -> null
        }

        private fun seededPolstJson(
            shortId: String,
            question: String,
            options: List<Pair<String, String>>,
            tallies: Map<String, Long>,
        ): String {
            val optionsJson = options.joinToString(",") { (id, label) ->
                """{"id":"$id","label":"$label"}"""
            }
            val talliesJson = tallies.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
            return """
                {
                  "short_id": "$shortId",
                  "question": "$question",
                  "options": [$optionsJson],
                  "tallies": {$talliesJson},
                  "brand": {"slug":"demo","display_name":"Demo Brand"},
                  "created_at": "2026-01-01T00:00:00Z",
                  "version": 1
                }
            """.trimIndent()
        }

        private fun json(status: Response.IStatus, body: String): Response =
            newFixedLengthResponse(status, "application/json; charset=utf-8", body).apply {
                addHeader("Cache-Control", "no-store")
            }

        companion object {
            private val OPTION_RE = Regex("\"option_id\"\\s*:\\s*\"([^\"]+)\"")
        }
    }
}
