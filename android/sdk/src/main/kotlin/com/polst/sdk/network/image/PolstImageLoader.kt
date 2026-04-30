package com.polst.sdk.network.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.polst.sdk.network.HttpMethod
import com.polst.sdk.network.HttpRequest
import com.polst.sdk.network.HttpResponse
import com.polst.sdk.network.HttpTransport
import com.polst.sdk.network.HttpUrlConnectionTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * First-party image loader.
 *
 * Constitution Principle II forbids third-party HTTP/image libraries, so this
 * class stitches together [HttpTransport] (HttpURLConnection under the hood),
 * [android.util.LruCache] for in-memory storage, and [DiskLruCache] for on-disk
 * persistence.
 *
 * Load pipeline:
 *  1. Memory cache hit — return immediately.
 *  2. De-duplicate concurrent requests for the same URL via a mutex-guarded
 *     `Map<String, Deferred<Bitmap?>>`.
 *  3. Disk cache hit (and not expired per `Cache-Control: max-age`) — decode and
 *     promote into memory.
 *  4. Network fetch via [HttpTransport], decode, write-through to disk + memory.
 *
 * Failures (IO, decode, non-2xx) degrade to `null`; image loads are never fatal.
 */
public class PolstImageLoader internal constructor(
    private val memoryCache: LruCache<String, Bitmap>,
    private val diskCache: DiskLruCache,
    private val diskDirectory: File,
    private val transport: HttpTransport,
) {
    private val inFlightMutex: Mutex = Mutex()
    private val inFlight: MutableMap<String, Deferred<Bitmap?>> = mutableMapOf()

    public suspend fun load(url: String): Bitmap? {
        memoryCache.get(url)?.let { return it }

        val key: String = sha256Hex(url)

        val (ownDeferred: CompletableDeferred<Bitmap?>?, existing: Deferred<Bitmap?>?) =
            inFlightMutex.withLock {
                val already: Deferred<Bitmap?>? = inFlight[url]
                if (already != null) {
                    null to already
                } else {
                    val fresh: CompletableDeferred<Bitmap?> = CompletableDeferred()
                    inFlight[url] = fresh
                    fresh to null
                }
            }

        if (existing != null) return existing.await()
        checkNotNull(ownDeferred)

        val result: Bitmap? = try {
            fetch(url, key)
        } catch (_: Throwable) {
            null
        } finally {
            inFlightMutex.withLock { inFlight.remove(url) }
        }
        ownDeferred.complete(result)
        return result
    }

    private suspend fun fetch(url: String, key: String): Bitmap? = withContext(Dispatchers.IO) {
        // Disk hit?
        val cached: File? = diskCache.get(key)
        if (cached != null) {
            val meta = File(diskDirectory, "$key.meta")
            val expired: Boolean = meta.exists() &&
                (meta.readText().trim().toLongOrNull() ?: Long.MAX_VALUE) < System.currentTimeMillis()
            if (!expired) {
                val decoded: Bitmap? = BitmapFactory.decodeFile(cached.absolutePath)
                if (decoded != null) {
                    memoryCache.put(url, decoded)
                    return@withContext decoded
                }
                // Corrupt on-disk entry — drop and refetch.
                diskCache.remove(key)
            } else {
                // Expired — try network; fall back to the stale file only if network fails.
                return@withContext refetchOrStale(url, key, cached)
            }
        }

        return@withContext network(url, key)
    }

    private suspend fun refetchOrStale(url: String, key: String, stale: File): Bitmap? {
        return try {
            network(url, key) ?: BitmapFactory.decodeFile(stale.absolutePath)
                ?.also { memoryCache.put(url, it) }
        } catch (_: Throwable) {
            BitmapFactory.decodeFile(stale.absolutePath)?.also { memoryCache.put(url, it) }
        }
    }

    private suspend fun network(url: String, key: String): Bitmap? {
        val response: HttpResponse = transport.send(HttpRequest(url = url, method = HttpMethod.GET))
        if (response.statusCode !in 200..299) return null
        val bytes: ByteArray = response.body ?: return null
        val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        diskCache.put(key, bytes)
        writeExpiryMetaIfPresent(key, response.headers)
        memoryCache.put(url, bitmap)
        return bitmap
    }

    private fun writeExpiryMetaIfPresent(key: String, headers: Map<String, List<String>>) {
        val cacheControl: String = headers.entries
            .firstOrNull { it.key.equals("Cache-Control", ignoreCase = true) }
            ?.value
            ?.joinToString(",")
            ?: return
        val maxAgeSeconds: Long = parseMaxAge(cacheControl) ?: return
        val expiryEpochMs: Long = System.currentTimeMillis() + (maxAgeSeconds * 1000L)
        try {
            File(diskDirectory, "$key.meta").writeText(expiryEpochMs.toString())
        } catch (_: IOException) {
            // Sidecar is best-effort; failure just means we won't honor max-age on next read.
        }
    }

    public companion object {
        public fun create(context: Context): PolstImageLoader {
            val maxMemory: Long = minOf(MAX_MEMORY_CACHE_BYTES, Runtime.getRuntime().maxMemory() / 8L)
            val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(maxMemory.toInt()) {
                override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
            }
            val diskDir = File(context.cacheDir, "polst-images")
            val diskCache = DiskLruCache(diskDir, MAX_DISK_CACHE_BYTES)
            return PolstImageLoader(
                memoryCache = memoryCache,
                diskCache = diskCache,
                diskDirectory = diskDir,
                transport = HttpUrlConnectionTransport(),
            )
        }

        private const val MAX_MEMORY_CACHE_BYTES: Long = 32L * 1024L * 1024L
        private const val MAX_DISK_CACHE_BYTES: Long = 50L * 1024L * 1024L
    }
}

private fun sha256Hex(input: String): String {
    val digest: ByteArray = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    val sb = StringBuilder(digest.size * 2)
    for (b in digest) {
        val v: Int = b.toInt() and 0xFF
        sb.append(HEX_CHARS[v ushr 4])
        sb.append(HEX_CHARS[v and 0x0F])
    }
    return sb.toString()
}

private val HEX_CHARS: CharArray = "0123456789abcdef".toCharArray()

private fun parseMaxAge(cacheControl: String): Long? {
    cacheControl.split(',').forEach { directive ->
        val trimmed: String = directive.trim()
        if (trimmed.startsWith("max-age", ignoreCase = true)) {
            val eq: Int = trimmed.indexOf('=')
            if (eq > 0) {
                val value: String = trimmed.substring(eq + 1).trim()
                return value.toLongOrNull()
            }
        }
    }
    return null
}
