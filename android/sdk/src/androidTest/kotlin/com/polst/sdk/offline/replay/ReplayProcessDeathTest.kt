package com.polst.sdk.offline.replay

// NOTE: This is the closest in-instrumentation approximation of process death;
// true cross-process resumption is verified manually. See MANUAL TEST PROCEDURE
// at the bottom of this file.

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
public class ReplayProcessDeathTest {

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
        dbFile = File(parentDir, "test_replay.db")
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
    public fun entryPersistsAcrossWorkerRunOnTransportFailure() {
        // Insert an entry with an endpoint that will never resolve, simulating
        // a vote enqueued before process death.
        val entry = ReplayEntity(
            idempotencyKey = "x",
            endpoint = "https://localhost.invalid/never-resolves",
            method = "POST",
            body = "{}".toByteArray(),
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = null,
            attemptCount = 0,
            lastErrorClass = null,
        )
        dao.insert(entry)
        assertEquals(1, dao.count())

        // Close the database — simulate process death.
        database.close()

        // Re-open the database from the same file — simulate process restart.
        database = Room.databaseBuilder(
            context,
            ReplayDatabase::class.java,
            dbFile.absolutePath,
        ).build()
        dao = database.replayDao()

        // Verify the entry survived the "process restart".
        assertEquals(1, dao.count())

        // Enqueue the worker and drive it via the test driver.
        val workRequest = OneTimeWorkRequestBuilder<ReplayWorker>().build()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest).result.get()

        WorkManagerTestInitHelper.getTestDriver(context)!!
            .setAllConstraintsMet(workRequest.id)

        // Await worker completion.
        val workInfo: WorkInfo = workManager
            .getWorkInfoByIdLiveData(workRequest.id)
            .let { liveData ->
                // Poll the synchronous future until terminal.
                var info = workManager.getWorkInfoById(workRequest.id)
                    .get(30, TimeUnit.SECONDS)
                val deadline = System.currentTimeMillis() + 30_000L
                while (info != null && !info.state.isFinished &&
                    System.currentTimeMillis() < deadline
                ) {
                    Thread.sleep(100)
                    info = workManager.getWorkInfoById(workRequest.id)
                        .get(5, TimeUnit.SECONDS)
                }
                info
            }

        assertNotNull(workInfo)
        assertTrue(
            "Worker should have finished, state=${workInfo.state}",
            workInfo.state.isFinished,
        )

        // The entry should NOT be deleted because the request failed.
        assertEquals(1, dao.count())

        // Verify the attempt was bumped: lastAttemptAt is non-null and
        // attemptCount has been incremented.
        val survivor = dao.listOrderedByCreated().single()
        assertNotNull(
            "lastAttemptAt should be set after a failed worker run",
            survivor.lastAttemptAt,
        )
        assertTrue(
            "attemptCount should be incremented (was ${survivor.attemptCount})",
            survivor.attemptCount >= 1,
        )
    }
}

// MANUAL TEST PROCEDURE:
// True process-death verification requires cross-process steps that cannot be
// performed inside a single instrumented test. To reproduce FR-054 end-to-end:
//
// 1. Install the example app on a physical device or emulator.
// 2. Enable airplane mode (or disable all network connectivity).
// 3. Submit a vote from the app — it will be enqueued into the Replay queue.
// 4. Force-stop the app from system settings (Settings -> Apps -> Force stop).
// 5. Disable airplane mode / restore network connectivity.
// 6. Relaunch the example app.
// 7. Observe that the vote appears in the backend within ~10 seconds, confirming
//    WorkManager resumed the enqueued request after process death.
