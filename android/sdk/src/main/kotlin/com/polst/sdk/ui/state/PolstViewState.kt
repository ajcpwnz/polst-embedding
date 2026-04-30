package com.polst.sdk.ui.state

import com.polst.sdk.Polst
import com.polst.sdk.Vote
import com.polst.sdk.network.PolstApiError
import java.util.UUID

/**
 * Finite state machine for a single-Polst UI surface.
 *
 * Modeled after the `PolstViewState` entry in `data-model.md`. The
 * [Loaded] state carries a [Freshness] flag so Compose UIs can decide
 * whether to render a "cached / offline" badge alongside the content.
 */
public sealed class PolstViewState {

    /** No Polst has been requested yet. */
    public data object Idle : PolstViewState()

    /** A fetch is in progress and no cached value is available. */
    public data object Loading : PolstViewState()

    /** A Polst has been rendered successfully. */
    public data class Loaded(
        val polst: Polst,
        val freshness: Freshness = Freshness.Fresh,
    ) : PolstViewState()

    /**
     * The user has tapped an option and the vote is in flight. The
     * [idempotencyKey] is captured here so the UI can correlate the
     * subsequent [Voted] state with the in-flight request.
     */
    public data class Voting(
        val polst: Polst,
        val idempotencyKey: UUID,
        val optionId: String,
    ) : PolstViewState()

    /** Terminal success state for the vote flow. */
    public data class Voted(
        val polst: Polst,
        val vote: Vote,
    ) : PolstViewState()

    /** Terminal error state for any phase of the flow. */
    public data class Error(
        val error: PolstApiError,
    ) : PolstViewState()
}

/**
 * Indicator for how fresh a [PolstViewState.Loaded] payload is. Used by
 * the UI layer to surface offline / stale badges without forcing the
 * domain layer to model freshness inside [Polst] itself.
 */
public enum class Freshness {
    Fresh,
    StaleOffline,
    StaleStale,
}
