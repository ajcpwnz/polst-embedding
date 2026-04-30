package com.polst.sdk.ui.locals

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.polst.sdk.PolstClient

/**
 * CompositionLocal carrying the active [PolstClient].
 *
 * Defaults to `null`; SDK composables fall back to [PolstClient.Companion.default]
 * (set via [PolstClient.Companion.installDefault]) when no explicit local is provided.
 * Hosts that need multiple clients (e.g. a brand-admin surface alongside a public one)
 * wrap the subtree in `CompositionLocalProvider(LocalPolstClient provides customClient)`.
 */
public val LocalPolstClient: ProvidableCompositionLocal<PolstClient?> =
    staticCompositionLocalOf<PolstClient?> { null }
