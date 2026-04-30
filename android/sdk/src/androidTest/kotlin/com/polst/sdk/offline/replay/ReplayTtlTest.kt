package com.polst.sdk.offline.replay

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
public class ReplayTtlTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var database: ReplayDatabase
    private lateinit var dao: ReplayDao

    @Before
    public fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        val parentDir = File(context.filesDir, "polst")
        parentDir.mkdirs()
        dbFile = File(parentDir, "test_replay_ttl.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        database = Room.databaseBuilder(
            context,
            ReplayDatabase::class.java,
            dbFile.absolutePath,
        ).build()
        dao = database.replayDao()
    }

    @After
    public fun tearDown() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
        if (::dbFile.isInitialized && dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    public fun entryOlderThanSevenDays_isDroppedAtDrainTime() {
        val now = System.currentTimeMillis()
        val eightDaysAgo = now - (8L * 24 * 60 * 60 * 1000)

        // Stale entry — 8 days old, should be dropped at drain time (FR-055).
        val stale = ReplayEntity(
            idempotencyKey = "stale",
            endpoint = "https://localhost.invalid/never-resolves",
            method = "POST",
            body = "{}".toByteArray(),
            createdAt = eightDaysAgo,
            lastAttemptAt = null,
            attemptCount = 0,
            lastErrorClass = null,
        )
        // Fresh entry — just created, should survive.
        val recent = ReplayEntity(
            idempotencyKey = "recent",
            endpoint = "https://localhost.invalid/never-resolves",
            method = "POST",
            body = "{}".toByteArray(),
            createdAt = now,
            lastAttemptAt = null,
            attemptCount = 0,
            lastErrorClass = null,
        )
        dao.insert(stale)
        dao.insert(recent)
        assertEquals(2, dao.count())

        // Trigger drain via the worker.
        val workRequest = OneTimeWorkRequestBuilder<ReplayWorker>().build()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest).result.get()

        WorkManagerTestInitHelper.getTestDriver(context)!!
            .setAllConstraintsMet(workRequest.id)

        // Await worker completion.
        var workInfo: WorkInfo? = workManager.getWorkInfoById(workRequest.id)
            .get(30, TimeUnit.SECONDS)
        val deadline = System.currentTimeMillis() + 30_000L
        while (workInfo != null && !workInfo.state.isFinished &&
            System.currentTimeMillis() < deadline
        ) {
            Thread.sleep(100)
            workInfo = workManager.getWorkInfoById(workRequest.id)
                .get(5, TimeUnit.SECONDS)
        }
        assertNotNull(workInfo)
        assertTrue(
            "Worker should have finished, state=${workInfo!!.state}",
            workInfo.state.isFinished,
        )

        // Only the recent entry should remain; the 8-day-old stale entry was
        // dropped at drain time per FR-055.
        assertEquals(1, dao.count())
        assertEquals(
            "recent",
            dao.listOrderedByCreated().single().idempotencyKey,
        )
    }
}
