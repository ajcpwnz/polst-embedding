package com.polst.sdk.network.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Minimal vendored on-disk LRU cache.
 *
 * Intentionally narrow — implements only what [PolstImageLoader] needs:
 * atomic writes, access-order eviction, and cooperative locking via a private [Mutex].
 *
 * Files are stored flat under [directory] keyed by caller-supplied hex keys
 * (see [PolstImageLoader] which uses `sha256(url)`). Sidecar `<key>.meta` files
 * carrying HTTP `Cache-Control` expiry metadata are tolerated (tracked as entries
 * for size accounting, never evicted ahead of their payload) but never created
 * by this class.
 */
internal class DiskLruCache(
    private val directory: File,
    private val maxSizeBytes: Long,
) {
    internal data class Entry(
        val key: String,
        val sizeBytes: Long,
        var accessedAt: Long,
    )

    private val mutex: Mutex = Mutex()

    // LinkedHashMap in access-order; eldest entry is the next eviction target.
    private val index: LinkedHashMap<String, Entry> = LinkedHashMap(16, 0.75f, true)

    private var currentSize: Long = 0L

    init {
        directory.mkdirs()
        val files: Array<File> = directory.listFiles() ?: emptyArray()
        // Seed the index ordered by lastModified ascending, so oldest entries
        // are first (eldest) in the access-ordered map.
        files.filter { it.isFile && !it.name.endsWith(".tmp") }
            .sortedBy { it.lastModified() }
            .forEach { file ->
                val entry = Entry(
                    key = file.name,
                    sizeBytes = file.length(),
                    accessedAt = file.lastModified(),
                )
                index[file.name] = entry
                currentSize += entry.sizeBytes
            }
        // Trim on startup in case maxSizeBytes shrank since last run.
        trimToSizeLocked()
    }

    suspend fun get(key: String): File? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entry: Entry = index[key] ?: return@withLock null
            val file = File(directory, key)
            if (!file.exists()) {
                index.remove(key)
                currentSize -= entry.sizeBytes
                return@withLock null
            }
            val now: Long = System.currentTimeMillis()
            entry.accessedAt = now
            file.setLastModified(now)
            file
        }
    }

    suspend fun put(key: String, bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        val tmp = File(directory, "$key.tmp")
        val target = File(directory, key)
        tmp.outputStream().use { it.write(bytes) }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.delete()
            return@withContext
        }
        mutex.withLock {
            val previous: Entry? = index.remove(key)
            if (previous != null) currentSize -= previous.sizeBytes
            val entry = Entry(
                key = key,
                sizeBytes = bytes.size.toLong(),
                accessedAt = System.currentTimeMillis(),
            )
            index[key] = entry
            currentSize += entry.sizeBytes
            trimToSizeLocked()
        }
    }

    suspend fun remove(key: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entry: Entry = index.remove(key) ?: return@withLock false
            currentSize -= entry.sizeBytes
            val file = File(directory, key)
            val meta = File(directory, "$key.meta")
            if (meta.exists()) meta.delete()
            file.delete()
        }
    }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            directory.listFiles()?.forEach { it.delete() }
            index.clear()
            currentSize = 0L
        }
    }

    /** Caller MUST hold [mutex]. */
    private fun trimToSizeLocked() {
        val iterator: MutableIterator<MutableMap.MutableEntry<String, Entry>> = index.entries.iterator()
        while (currentSize > maxSizeBytes && iterator.hasNext()) {
            val eldest: MutableMap.MutableEntry<String, Entry> = iterator.next()
            val entry: Entry = eldest.value
            iterator.remove()
            currentSize -= entry.sizeBytes
            File(directory, entry.key).delete()
            File(directory, "${entry.key}.meta").delete()
        }
    }
}
