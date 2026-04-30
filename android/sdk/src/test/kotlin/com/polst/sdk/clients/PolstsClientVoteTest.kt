package com.polst.sdk.clients

import com.polst.sdk.Environment
import com.polst.sdk.VoteState
import com.polst.sdk.core.deviceid.DeviceIdProvider
import com.polst.sdk.core.tokens.TokenSnapshot
import com.polst.sdk.core.tokens.TokenStore
import com.polst.sdk.network.HttpMethod
import com.polst.sdk.network.HttpRequest
import com.polst.sdk.network.HttpResponse
import com.polst.sdk.network.PolstApiError
import com.polst.sdk.network.RestClient
import com.polst.sdk.network.mockHttpTransport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

public class PolstsClientVoteTest {

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

    @Test
    public fun vote_returnsAcknowledgedOnSuccess(): Unit = runTest {
        val captured = AtomicReference<HttpRequest?>(null)
        val transport = mockHttpTransport {
            on(HttpMethod.POST, "/polsts/abc/votes") { request ->
                captured.set(request)
                HttpResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = """{"data":{"tallies":{"optionA":1,"optionB":0,"total":1},"isRevote":false,"votedAt":"2026-01-01T00:00:00Z"}}"""
                        .toByteArray(Charsets.UTF_8),
                )
            }
        }
        val client = PolstsClient(newRest(transport))

        val vote = client.vote(shortId = "abc", optionId = "A")

        assertNotNull(captured.get())
        assertEquals("abc", vote.polstShortId)
        assertEquals("A", vote.optionId)
        assertTrue(
            "expected Acknowledged, got ${vote.state}",
            vote.state is VoteState.Acknowledged,
        )
    }

    @Test
    public fun vote_sendsIdempotencyKeyHeader(): Unit = runTest {
        val captured = AtomicReference<HttpRequest?>(null)
        val transport = mockHttpTransport {
            on(HttpMethod.POST, "/polsts/abc/votes") { request ->
                captured.set(request)
                HttpResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = """{"data":{"tallies":{"optionA":1,"optionB":0,"total":1},"isRevote":false,"votedAt":"2026-01-01T00:00:00Z"}}"""
                        .toByteArray(Charsets.UTF_8),
                )
            }
        }
        val client = PolstsClient(newRest(transport))

        val vote = client.vote(shortId = "abc", optionId = "A")

        val request = captured.get()
        assertNotNull(request)
        val headerValue = request!!.headers.entries
            .firstOrNull { it.key.equals("X-Polst-Idempotency-Key", ignoreCase = true) }
            ?.value
        assertNotNull("Idempotency-Key header must be present", headerValue)
        val parsed = UUID.fromString(headerValue)
        assertEquals(vote.idempotencyKey, parsed)
    }

    @Test
    public fun vote_propagatesValidationOn422(): Unit = runTest {
        val body = """{"error":{"code":"VALIDATION","message":"Invalid.","fields":{"option":"required"}}}"""
        val transport = mockHttpTransport {
            on(HttpMethod.POST, "/polsts/abc/votes") {
                HttpResponse(
                    statusCode = 422,
                    headers = emptyMap(),
                    body = body.toByteArray(Charsets.UTF_8),
                )
            }
        }
        val client = PolstsClient(newRest(transport))

        var thrown: PolstApiError.Validation? = null
        try {
            client.vote(shortId = "abc", optionId = "A")
        } catch (e: PolstApiError.Validation) {
            thrown = e
        }
        assertNotNull("Expected PolstApiError.Validation", thrown)
        assertEquals(mapOf("option" to "required"), thrown!!.fieldErrors)
    }
}
