package com.polst.sdk.offline.replay

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.polst.sdk.offline.ConnectivityObserver
import com.polst.sdk.offline.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Schedules [ReplayWorker] on connectivity transitions to [NetworkState.Available]
 * (FR-053) and on explicit drain requests (FR-054 — enqueue-on-vote).
 *
 * WorkManager handles process-death survival (FR-054).
 */
internal class ReplayScheduler(
    private val context: Context,
    private val connectivity: ConnectivityObserver,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    private var lastState: NetworkState? = null

    /**
     * Begin observing connectivity. Idempotent-friendly (call once from PolstClient init).
     * On each transition from a non-Available state to [NetworkState.Available],
     * enqueues a drain.
     */
    internal fun start() {
        scope.launch {
            connectivity.observe().collect { current ->
                val previous = lastState
                if (current == NetworkState.Available && previous != NetworkState.Available) {
                    enqueueDrain()
                }
                lastState = current
            }
        }
    }

    /**
     * Manually request a drain — called from PolstsClient.vote after enqueueing a new
     * replay entry, to attempt immediate delivery if the network is currently up.
     */
    internal fun enqueueNow() {
        enqueueDrain()
    }

    /** Stop observing connectivity; cancels the scope. */
    internal fun stop() {
        scope.cancel()
    }

    private fun enqueueDrain() {
        val request = OneTimeWorkRequestBuilder<ReplayWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        // KEEP policy: if a drain is already scheduled, don't enqueue another.
        // Prevents thrash on rapid connectivity flips.
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(ReplayWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
