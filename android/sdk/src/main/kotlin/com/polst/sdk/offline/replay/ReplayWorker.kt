package com.polst.sdk.offline.replay

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.polst.sdk.network.HttpMethod
import com.polst.sdk.network.HttpRequest
import com.polst.sdk.network.HttpTransport
import com.polst.sdk.network.HttpUrlConnectionTransport
import java.io.IOException

/**
 * WorkManager worker that drains the offline replay queue.
 *
 * FIFO ordering (by createdAt) — see FR-052..056.
 *
 * NOTE: entries in [ReplayEntity.endpoint] store the FULL URL (e.g.
 * "https://api.polst.app/api/rest/v1/polsts/{id}/votes") because the worker may run
 * in a separate process and does not have access to the Environment configured on
 * PolstClient. PolstsClient.vote is responsible for persisting the absolute URL.
 */
internal class ReplayWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Fresh transport: worker may run in a separate process where PolstClient
        // is not initialized. HttpUrlConnectionTransport is small and stateless.
        val db = ReplayDatabase.getInstance(applicationContext)
        val dao = db.replayDao()
        val transport: HttpTransport = HttpUrlConnectionTransport()

        // Step 1: TTL prune (FR-055 diagnostic).
        val cutoffMs = System.currentTimeMillis() - SEVEN_DAYS_MS
        val droppedCount = dao.deleteOlderThan(cutoffMs)
        if (droppedCount > 0) {
            Log.w(LOG_TAG, "Dropped $droppedCount entries older than 7 days")
        }

        // Step 2: FIFO drain.
        val pending = dao.listOrderedByCreated()
        var networkDown = false
        for (entry in pending) {
            val request = HttpRequest(
                url = entry.endpoint,
                method = HttpMethod.valueOf(entry.method),
                headers = mapOf(
                    "Idempotency-Key" to entry.idempotencyKey,
                    "Content-Type" to "application/json; charset=utf-8",
                    "Accept" to "application/json",
                ),
                body = entry.body,
            )

            try {
                val response = transport.send(request)
                when (ReplayWorkerLogic.classify(response.statusCode)) {
                    ReplayWorkerLogic.Action.Delete -> {
                        // 2xx or 409 — 409 means backend already counted this vote (FR-056).
                        dao.deleteByKey(entry.idempotencyKey)
                    }
                    ReplayWorkerLogic.Action.Bump -> {
                        // Other 4xx or 5xx — record failure, keep entry for future drain.
                        dao.bumpAttempt(
                            entry.idempotencyKey,
                            System.currentTimeMillis(),
                            "HttpStatus${response.statusCode}",
                        )
                    }
                    ReplayWorkerLogic.Action.Retry -> {
                        // Unknown status (e.g. 1xx/3xx) — bump and continue.
                        dao.bumpAttempt(
                            entry.idempotencyKey,
                            System.currentTimeMillis(),
                            "HttpStatus${response.statusCode}",
                        )
                    }
                }
            } catch (io: IOException) {
                // Network went down mid-drain — record failure and stop; WorkManager will retry.
                dao.bumpAttempt(
                    entry.idempotencyKey,
                    System.currentTimeMillis(),
                    "IOException:${io.javaClass.simpleName}",
                )
                networkDown = true
                break
            }
        }

        return if (networkDown) Result.retry() else Result.success()
    }

    internal companion object {
        internal const val WORK_NAME: String = "polst.replay.worker"
        private const val SEVEN_DAYS_MS: Long = 7L * 24 * 60 * 60 * 1000
        private const val LOG_TAG: String = "Polst-Replay"
    }
}

/**
 * Pure classifier extracted for unit-testability without Android/Room/WorkManager.
 * See [ReplayWorkerLogicTest].
 */
internal object ReplayWorkerLogic {
    internal enum class Action { Delete, Bump, Retry }

    internal fun classify(statusCode: Int): Action = when {
        statusCode in 200..299 || statusCode == 409 -> Action.Delete
        statusCode in 400..499 -> Action.Bump
        statusCode in 500..599 -> Action.Bump
        else -> Action.Retry
    }
}
