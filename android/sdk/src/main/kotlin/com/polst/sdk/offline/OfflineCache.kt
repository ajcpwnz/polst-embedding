package com.polst.sdk.offline

import android.content.Context
import java.util.logging.Logger
import com.polst.sdk.network.dto.BrandFeedPageDto
import com.polst.sdk.network.dto.CampaignDto
import com.polst.sdk.network.dto.PolstDto
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
internal data class IndexEntry(
    val etag: String? = null,
    val writtenAt: Long,
    val freshness: String = "fresh",
)

internal enum class CacheFreshness {
    Fresh,
    Stale,
    StaleOffline,
}

internal data class CachedEntry<T>(
    val payload: T,
    val etag: String?,
    val writtenAt: Instant,
    val freshness: CacheFreshness,
)

internal class OfflineCache
internal constructor(
    private val rootDir: File,
    private val json: Json = defaultJson(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    internal constructor(
        context: Context,
        json: Json = defaultJson(),
        clock: () -> Long = { System.currentTimeMillis() },
    ) : this(
        rootDir = File(context.filesDir, "polst/offline").apply { mkdirs() },
        json = json,
        clock = clock,
    )

    init {
        rootDir.mkdirs()
    }

    private val indexFile: File = File(rootDir, "_index.json")
    private val mutex: Mutex = Mutex()

    private var indexCache: MutableMap<String, IndexEntry>? = null

    internal suspend fun writePolst(dto: PolstDto, etag: String? = null) {
        val name = "polst_${dto.shortId}.json"
        writeEntry(name, json.encodeToString(PolstDto.serializer(), dto), etag)
    }

    internal suspend fun readPolst(shortId: String): CachedEntry<PolstDto>? {
        val name = "polst_${shortId}.json"
        return readEntry(name) { raw -> json.decodeFromString(PolstDto.serializer(), raw) }
    }

    internal suspend fun writeCampaign(dto: CampaignDto, etag: String? = null) {
        val name = "campaign_${dto.id}.json"
        writeEntry(name, json.encodeToString(CampaignDto.serializer(), dto), etag)
    }

    internal suspend fun readCampaign(campaignId: String): CachedEntry<CampaignDto>? {
        val name = "campaign_${campaignId}.json"
        return readEntry(name) { raw -> json.decodeFromString(CampaignDto.serializer(), raw) }
    }

    internal suspend fun writeBrandFeedPage(
        brandSlug: String,
        cursor: String?,
        dto: BrandFeedPageDto,
        etag: String? = null,
    ) {
        val name = "brandfeed_${brandSlug}_page_${cursor ?: "initial"}.json"
        writeEntry(name, json.encodeToString(BrandFeedPageDto.serializer(), dto), etag)
    }

    internal suspend fun readBrandFeedPage(
        brandSlug: String,
        cursor: String?,
    ): CachedEntry<BrandFeedPageDto>? {
        val name = "brandfeed_${brandSlug}_page_${cursor ?: "initial"}.json"
        return readEntry(name) { raw -> json.decodeFromString(BrandFeedPageDto.serializer(), raw) }
    }

    internal suspend fun clear() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                rootDir.listFiles()?.forEach { it.delete() }
                indexCache = mutableMapOf()
            }
        }
    }

    private suspend fun writeEntry(name: String, payloadJson: String, etag: String?) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val index = loadIndexLocked()
                val payloadFile = File(rootDir, name)
                val tmpFile = File(rootDir, "$name.tmp")
                tmpFile.writeText(payloadJson)
                if (payloadFile.exists()) {
                    payloadFile.delete()
                }
                if (!tmpFile.renameTo(payloadFile)) {
                    // Fallback: copy + delete
                    payloadFile.writeText(tmpFile.readText())
                    tmpFile.delete()
                }
                index[name] = IndexEntry(
                    etag = etag,
                    writtenAt = clock(),
                    freshness = "fresh",
                )
                writeIndexLocked(index)
            }
        }
    }

    private suspend fun <T> readEntry(
        name: String,
        decode: (String) -> T,
    ): CachedEntry<T>? {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val index = loadIndexLocked()
                val entry = index[name] ?: return@withContext null
                val payloadFile = File(rootDir, name)
                if (!payloadFile.exists()) return@withContext null
                val raw = try {
                    payloadFile.readText()
                } catch (t: Throwable) {
                    log.warning("Failed to read cache payload $name: ${t.message}")
                    return@withContext null
                }
                val payload = try {
                    decode(raw)
                } catch (e: SerializationException) {
                    log.warning("Failed to decode cache payload $name: ${e.message}")
                    return@withContext null
                }
                val ageMs = clock() - entry.writtenAt
                val freshness = if (ageMs > ONE_HOUR_MS) CacheFreshness.Stale else CacheFreshness.Fresh
                CachedEntry(
                    payload = payload,
                    etag = entry.etag,
                    writtenAt = Instant.ofEpochMilli(entry.writtenAt),
                    freshness = freshness,
                )
            }
        }
    }

    private fun loadIndexLocked(): MutableMap<String, IndexEntry> {
        val cached = indexCache
        if (cached != null) return cached
        val loaded: MutableMap<String, IndexEntry> = if (indexFile.exists()) {
            try {
                val raw = indexFile.readText()
                if (raw.isBlank()) {
                    mutableMapOf()
                } else {
                    json.decodeFromString(
                        MapSerializer(String.serializer(), IndexEntry.serializer()),
                        raw,
                    ).toMutableMap()
                }
            } catch (e: SerializationException) {
                log.warning("Corrupt _index.json, resetting: ${e.message}")
                mutableMapOf()
            } catch (t: Throwable) {
                log.warning("Failed to load _index.json: ${t.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
        indexCache = loaded
        return loaded
    }

    private fun writeIndexLocked(index: Map<String, IndexEntry>) {
        val serialized = json.encodeToString(
            MapSerializer(String.serializer(), IndexEntry.serializer()),
            index,
        )
        val tmp = File(rootDir, "_index.json.tmp")
        tmp.writeText(serialized)
        if (indexFile.exists()) {
            indexFile.delete()
        }
        if (!tmp.renameTo(indexFile)) {
            indexFile.writeText(tmp.readText())
            tmp.delete()
        }
    }

    internal companion object {
        private val log: Logger = Logger.getLogger("Polst-OfflineCache")
        private const val ONE_HOUR_MS: Long = 60L * 60L * 1000L

        private fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    }
}
