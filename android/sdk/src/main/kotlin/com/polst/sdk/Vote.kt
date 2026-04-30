package com.polst.sdk

import java.time.Instant
import java.util.UUID

public data class Vote(
    val polstShortId: String,
    val optionId: String,
    val idempotencyKey: UUID,
    val castAt: Instant,
    val state: VoteState,
)

public sealed class VoteState {
    public data object Pending : VoteState()
    public data class Acknowledged(val acknowledgedAt: Instant) : VoteState()
    public data class DroppedAfterDeadline(val droppedAt: Instant, val attemptCount: Int) : VoteState()
}
