package com.polst.sdk.offline

import com.polst.sdk.network.dto.BrandSummaryDto
import com.polst.sdk.network.dto.PolstDto
import com.polst.sdk.network.dto.PolstOptionDto
import java.io.File
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

public class OfflineCacheTest {

    @get:Rule
    public val tempFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var rootDir: File

    private val samplePolst: PolstDto = PolstDto(
        shortId = "abc123",
        title = "Demo question?",
        optionA = PolstOptionDto(label = "A"),
        optionB = PolstOptionDto(label = "B"),
        tallies = null,
        brand = BrandSummaryDto(slug = "demo", name = "Demo Brand"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Before
    public fun setUp() {
        rootDir = File(tempFolder.root, "polst/offline").apply { mkdirs() }
    }

    @After
    public fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    public fun writeAndReadPolst_roundTripsPayloadAndEtag(): Unit = runTest {
        val cache = OfflineCache(rootDir = rootDir, json = json, clock = { 1_000L })
        cache.writePolst(samplePolst, etag = "etag-1")

        val result = cache.readPolst("abc123")

        assertNotNull(result)
        assertEquals(samplePolst.shortId, result!!.payload.shortId)
        assertEquals("etag-1", result.etag)
        assertEquals(CacheFreshness.Fresh, result.freshness)
    }

    @Test
    public fun readMissing_returnsNull(): Unit = runTest {
        val cache = OfflineCache(rootDir = rootDir, json = json, clock = { 1_000L })

        val result = cache.readPolst("missing")

        assertNull(result)
    }

    @Test
    public fun freshnessIsFreshForRecentWrite(): Unit = runTest {
        var now = 10_000L
        val cache = OfflineCache(rootDir = rootDir, json = json, clock = { now })
        cache.writePolst(samplePolst)

        now += 5 * 60 * 1000L // +5 minutes
        val result = cache.readPolst("abc123")

        assertNotNull(result)
        assertEquals(CacheFreshness.Fresh, result!!.freshness)
    }

    @Test
    public fun freshnessIsStaleAfterOneHour(): Unit = runTest {
        var now = 10_000L
        val cache = OfflineCache(rootDir = rootDir, json = json, clock = { now })
        cache.writePolst(samplePolst)

        now += 60 * 60 * 1000L + 1 // just past one hour
        val result = cache.readPolst("abc123")

        assertNotNull(result)
        assertEquals(CacheFreshness.Stale, result!!.freshness)
    }

    @Test
    public fun clear_removesAllEntries(): Unit = runTest {
        val cache = OfflineCache(rootDir = rootDir, json = json, clock = { 1_000L })
        cache.writePolst(samplePolst)

        cache.clear()

        assertNull(cache.readPolst("abc123"))
        val leftover = rootDir.listFiles()?.toList().orEmpty()
        assertEquals(0, leftover.size)
    }

    @Test
    public fun corruptIndexFile_doesNotCrash(): Unit = runTest {
        val indexFile = File(rootDir, "_index.json")
        indexFile.writeText("{ this is not valid json")

        val cache = OfflineCache(rootDir = rootDir, json = json, clock = { 1_000L })

        // Reads should return null without throwing
        assertNull(cache.readPolst("abc123"))

        // Writes should recover and persist
        cache.writePolst(samplePolst, etag = "recovered")
        val result = cache.readPolst("abc123")
        assertNotNull(result)
        assertEquals("recovered", result!!.etag)
    }
}
