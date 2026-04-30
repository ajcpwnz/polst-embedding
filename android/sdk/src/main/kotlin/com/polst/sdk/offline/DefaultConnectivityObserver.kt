package com.polst.sdk.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * [ConnectivityObserver] implementation backed by
 * [android.net.ConnectivityManager.NetworkCallback].
 *
 * Only the application context is retained to avoid leaking Activity
 * references.
 */
public class DefaultConnectivityObserver(context: Context) : ConnectivityObserver {

    private val appContext: Context = context.applicationContext

    override fun observe(): Flow<NetworkState> = callbackFlow {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkState.Unavailable)
            }

            override fun onUnavailable() {
                trySend(NetworkState.Unavailable)
            }
        }

        // Seed the flow with the current state so collectors don't have to
        // wait for the next connectivity transition.
        val initial = cm.activeNetwork
            ?.let { cm.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(if (initial) NetworkState.Available else NetworkState.Unavailable)

        cm.registerNetworkCallback(request, callback)

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}
