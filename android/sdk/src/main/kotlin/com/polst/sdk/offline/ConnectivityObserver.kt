package com.polst.sdk.offline

import kotlinx.coroutines.flow.Flow

/**
 * Coarse-grained network reachability states emitted by [ConnectivityObserver].
 *
 * The observer collapses Android's richer connectivity model into just two
 * values — the SDK only needs to know whether it can attempt a network call.
 */
public enum class NetworkState {
    /** A network with internet capability is currently available. */
    Available,

    /** No network with internet capability is currently available. */
    Unavailable,
}

/**
 * Observes device connectivity and exposes it as a cold [Flow] of
 * [NetworkState] values.
 *
 * Implementations MUST emit an initial value on collection so consumers do
 * not have to wait for the next connectivity change to learn the current
 * state.
 */
public fun interface ConnectivityObserver {
    /** Returns a cold [Flow] of [NetworkState] updates. */
    public fun observe(): Flow<NetworkState>
}
