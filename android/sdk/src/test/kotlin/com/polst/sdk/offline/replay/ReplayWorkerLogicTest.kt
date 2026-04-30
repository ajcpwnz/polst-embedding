package com.polst.sdk.offline.replay

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ReplayWorkerLogic.classify] — the pure response-classification
 * function extracted from [ReplayWorker] to allow JVM testing without Android,
 * Room, or WorkManager.
 *
 * Covers FR-056 (409 treated as success / at-most-once per idempotency key).
 */
internal class ReplayWorkerLogicTest {

    @Test
    fun classify_2xx_isDelete() {
        assertEquals(ReplayWorkerLogic.Action.Delete, ReplayWorkerLogic.classify(200))
        assertEquals(ReplayWorkerLogic.Action.Delete, ReplayWorkerLogic.classify(201))
        assertEquals(ReplayWorkerLogic.Action.Delete, ReplayWorkerLogic.classify(204))
        assertEquals(ReplayWorkerLogic.Action.Delete, ReplayWorkerLogic.classify(299))
    }

    @Test
    fun classify_409_isDelete() {
        // FR-056: backend has already counted this vote (at-most-once semantics).
        // Treat 409 Conflict as a successful drain and remove the entry.
        assertEquals(ReplayWorkerLogic.Action.Delete, ReplayWorkerLogic.classify(409))
    }

    @Test
    fun classify_4xx_isBump() {
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(400))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(401))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(403))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(404))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(429))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(499))
    }

    @Test
    fun classify_5xx_isBump() {
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(500))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(502))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(503))
        assertEquals(ReplayWorkerLogic.Action.Bump, ReplayWorkerLogic.classify(599))
    }
}
