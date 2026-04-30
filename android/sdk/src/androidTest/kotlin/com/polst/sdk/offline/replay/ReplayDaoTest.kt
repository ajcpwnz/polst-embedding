package com.polst.sdk.offline.replay

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

public class ReplayDaoTest {

    private lateinit var database: ReplayDatabase
    private lateinit var dao: ReplayDao

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ReplayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.replayDao()
    }

    @After
    public fun tearDown() {
        database.close()
    }

    private fun entry(
        key: String,
        createdAt: Long = 1_000L,
        attemptCount: Int = 0,
        lastAttemptAt: Long? = null,
        lastErrorClass: String? = null,
    ): ReplayEntity = ReplayEntity(
        idempotencyKey = key,
        endpoint = "/v1/events",
        method = "POST",
        body = "payload-$key".toByteArray(),
        createdAt = createdAt,
        lastAttemptAt = lastAttemptAt,
        attemptCount = attemptCount,
        lastErrorClass = lastErrorClass,
    )

    @Test
    public fun insert_thenList_returnsInsertedEntry(): Unit = runTest {
        dao.insert(entry("k1", createdAt = 10L))
        dao.insert(entry("k2", createdAt = 20L))

        val all = dao.listOrderedByCreated()

        assertEquals(2, all.size)
        assertEquals("k1", all[0].idempotencyKey)
        assertEquals("k2", all[1].idempotencyKey)
    }

    @Test
    public fun bumpAttempt_incrementsCount(): Unit = runTest {
        dao.insert(entry("k1", createdAt = 10L, attemptCount = 2))

        val updated = dao.bumpAttempt("k1", nowMs = 555L, errorClass = "IOException")

        assertEquals(1, updated)
        val all = dao.listOrderedByCreated()
        assertEquals(1, all.size)
        val row = all.first()
        assertEquals(3, row.attemptCount)
        assertEquals(555L, row.lastAttemptAt)
        assertEquals("IOException", row.lastErrorClass)
    }

    @Test
    public fun deleteByKey_removesEntry(): Unit = runTest {
        dao.insert(entry("k1"))
        dao.insert(entry("k2"))

        val removed = dao.deleteByKey("k1")

        assertEquals(1, removed)
        val remaining = dao.listOrderedByCreated()
        assertEquals(1, remaining.size)
        assertEquals("k2", remaining.first().idempotencyKey)
        assertNull(remaining.find { it.idempotencyKey == "k1" })
    }

    @Test
    public fun deleteOlderThan_prunesOldEntries(): Unit = runTest {
        dao.insert(entry("old1", createdAt = 100L))
        dao.insert(entry("old2", createdAt = 200L))
        dao.insert(entry("new1", createdAt = 500L))

        val pruned = dao.deleteOlderThan(300L)

        assertEquals(2, pruned)
        val remaining = dao.listOrderedByCreated()
        assertEquals(1, remaining.size)
        assertEquals("new1", remaining.first().idempotencyKey)
        assertNotNull(remaining.first())
        assertEquals(1, dao.count())
    }
}
