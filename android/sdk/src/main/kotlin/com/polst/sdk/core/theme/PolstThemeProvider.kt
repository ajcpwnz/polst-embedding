package com.polst.sdk.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.polst.sdk.ui.locals.LocalPolstTheme

/**
 * Provides a [PolstTheme] to the composition via [LocalPolstTheme].
 *
 * Host apps should wrap the Polst SDK composables in this provider at the
 * level at which they want the theme to apply. Nested providers simply
 * override the theme for their subtree.
 *
 * @param theme The [PolstTheme] tokens to expose through [LocalPolstTheme].
 * @param content The composable subtree that should read the provided theme.
 */
@Composable
public fun PolstThemeProvider(
    theme: PolstTheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPolstTheme provides theme) {
        content()
    }
}
