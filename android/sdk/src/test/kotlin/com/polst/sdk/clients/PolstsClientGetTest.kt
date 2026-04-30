package com.polst.sdk.clients

import com.polst.sdk.Environment
import com.polst.sdk.core.deviceid.DeviceIdProvider
import com.polst.sdk.core.tokens.TokenSnapshot
import com.polst.sdk.core.tokens.TokenStore
import com.polst.sdk.network.HttpMethod
import com.polst.sdk.network.HttpResponse
import com.polst.sdk.network.PolstApiError
import com.polst.sdk.network.RestClient
import com.polst.sdk.network.mockHttpTransport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

public class PolstsClientGetTest {

    private class FakeDeviceIdProvider(
        private val uuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    ) : DeviceIdProvider {
        override fun deviceId(): UUID = uuid
    }

    private class FakeTokenStore(private val snapshot: TokenSnapshot? = null) : TokenStore {
        override suspend fun read(): TokenSnapshot? = snapshot
        override suspend fun write(snapshot: TokenSnapshot) {}
        override suspend fun clear() {}
    }

    private fun newRest(
        transport: com.polst.sdk.network.HttpTransport,
    ): RestClient = RestClient(
        transport = transport,
        environment = Environment.Production,
        deviceIdProvider = FakeDeviceIdProvider(),
        tokenStore = FakeTokenStore(),
    )

    private fun polstJson(shortId: String): String = """
        {
          "data": {
            "shortId": "$shortId",
            "title": "Pick one?",
            "optionA": {"label":"A","imageUrl":null},
            "optionB": {"label":"B","imageUrl":null},
            "tallies": {"optionA": 0, "optionB": 0, "total": 0},
            "brand": {"slug":"acme","name":"Acme"},
            "createdAt": "2026-01-01T00:00:00Z"
          }
        }
    """.trimIndent()

    @Test
    public fun get_returnsPolstOn200(): Unit = runTest {
        val transport = mockHttpTransport {
            on(HttpMethod.GET, "/polsts/abc") {
                HttpResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = polstJson("abc").toByteArray(Charsets.UTF_8),
                )
            }
        }
        val client = PolstsClient(newRest(transport))

        val polst = client.get("abc")

        assertEquals("abc", polst.shortId)
        assertEquals("Pick one?", polst.question)
        assertEquals(2, polst.options.size)
        assertEquals("A", polst.options[0].id)
        assertEquals("B", polst.options[1].id)
    }

    @Test
    public fun get_throwsNotFoundOn404(): Unit = runTest {
        val body = """{"error":{"code":"NOT_FOUND","message":"Polst not found.","details":null}}"""
        val transport = mockHttpTransport {
            on(HttpMethod.GET, "/polsts/abc") {
                HttpResponse(
                    statusCode = 404,
                    headers = emptyMap(),
                    body = body.toByteArray(Charsets.UTF_8),
                )
            }
        }
        val client = PolstsClient(newRest(transport))

        var thrown: PolstApiError.NotFound? = null
        try {
            client.get("abc")
        } catch (e: PolstApiError.NotFound) {
            thrown = e
        }
        assertNotNull("Expected PolstApiError.NotFound", thrown)
        assertEquals("polst", thrown!!.resource)
    }

    @Test
    public fun get_retries5xxThenThrowsServerError(): Unit = runTest {
        val calls = AtomicInteger(0)
        val transport = mockHttpTransport {
            on(HttpMethod.GET, "/polsts/abc") {
                calls.incrementAndGet()
                HttpResponse(
                    statusCode = 500,
                    headers = emptyMap(),
                    body = "boom".toByteArray(Charsets.UTF_8),
                )
            }
        }
        val client = PolstsClient(newRest(transport))

        var thrown: PolstApiError.ServerError? = null
        try {
            client.get("abc")
        } catch (e: PolstApiError.ServerError) {
            thrown = e
        }
        assertNotNull("Expected PolstApiError.ServerError", thrown)
        assertEquals(500, thrown!!.code)
        assertEquals(4, calls.get())
    }
}
