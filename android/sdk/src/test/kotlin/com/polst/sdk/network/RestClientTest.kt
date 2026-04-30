package com.polst.sdk.network

import com.polst.sdk.Environment
import com.polst.sdk.core.deviceid.DeviceIdProvider
import com.polst.sdk.core.tokens.TokenSnapshot
import com.polst.sdk.core.tokens.TokenStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

public class RestClientTest {

    @Serializable
    private data class TestDto(val id: String)

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

    private fun newClient(transport: HttpTransport): RestClient = RestClient(
        transport = transport,
        environment = Environment.Custom("https://api.example.test/v1"),
        deviceIdProvider = FakeDeviceIdProvider(),
        tokenStore = FakeTokenStore(),
    )

    private fun okResponse(jsonBody: String): HttpResponse = HttpResponse(
        statusCode = 200,
        headers = emptyMap(),
        body = jsonBody.toByteArray(Charsets.UTF_8),
    )

    @Test
    public fun get_returnsDecodedResponseOn200(): Unit = runTest {
        val transport = MockHttpTransport { okResponse("""{"id":"x"}""") }
        val client = newClient(transport)

        val result: TestDto = client.get("/test")

        assertEquals(TestDto(id = "x"), result)
    }

    @Test
    public fun get_retries5xxThreeTimesThenThrowsServerError(): Unit = runTest {
        val calls = AtomicInteger(0)
        val transport = MockHttpTransport {
            calls.incrementAndGet()
            HttpResponse(statusCode = 500, headers = emptyMap(), body = "oops".toByteArray())
        }
        val client = newClient(transport)

        try {
            client.get<TestDto>("/test")
            fail("Expected ServerError")
        } catch (e: PolstApiError.ServerError) {
            assertEquals(500, e.code)
        }
        assertEquals(4, calls.get())
    }

    @Test
    public fun get_retriesIOExceptionThenSucceedsOnLastAttempt(): Unit = runTest {
        val calls = AtomicInteger(0)
        val transport = MockHttpTransport {
            val n = calls.incrementAndGet()
            if (n < 4) throw IOException("boom $n")
            okResponse("""{"id":"late"}""")
        }
        val client = newClient(transport)

        val result: TestDto = client.get("/test")

        assertEquals(TestDto(id = "late"), result)
        assertEquals(4, calls.get())
    }

    @Test
    public fun get_doesNotRetry4xx(): Unit = runTest {
        val calls = AtomicInteger(0)
        val transport = MockHttpTransport {
            calls.incrementAndGet()
            HttpResponse(statusCode = 400, headers = emptyMap(), body = "{}".toByteArray())
        }
        val client = newClient(transport)

        try {
            client.get<TestDto>("/test")
            fail("Expected HttpStatus")
        } catch (e: PolstApiError.HttpStatus) {
            assertEquals(400, e.code)
        } catch (e: PolstApiError.Validation) {
            // Acceptable if a validation body were present; with "{}" we expect HttpStatus though.
            fail("Unexpected Validation: $e")
        }
        assertEquals(1, calls.get())
    }

    @Test
    public fun get_mapsValidationFromBody(): Unit = runTest {
        val body = """{"error":{"code":"VALIDATION","message":"Invalid.","fields":{"option_id":"required"}}}"""
        val transport = MockHttpTransport {
            HttpResponse(statusCode = 422, headers = emptyMap(), body = body.toByteArray())
        }
        val client = newClient(transport)

        try {
            client.get<TestDto>("/test")
            fail("Expected Validation")
        } catch (e: PolstApiError.Validation) {
            assertEquals(mapOf("option_id" to "required"), e.fieldErrors)
        }
    }

    @Test
    public fun get_mapsRetryAfterFromHeader(): Unit = runTest {
        val transport = MockHttpTransport {
            HttpResponse(
                statusCode = 429,
                headers = mapOf("Retry-After" to listOf("30")),
                body = null,
            )
        }
        val client = newClient(transport)

        try {
            client.get<TestDto>("/test")
            fail("Expected RateLimited")
        } catch (e: PolstApiError.RateLimited) {
            assertNotNull(e.retryAfter)
            assertEquals(30.seconds, e.retryAfter)
            assertTrue(true)
        }
    }
}
