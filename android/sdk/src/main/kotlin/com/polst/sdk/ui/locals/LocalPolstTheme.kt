package com.polst.sdk.ui.locals

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.polst.sdk.core.theme.PolstTheme
import com.polst.sdk.core.theme.system

/**
 * CompositionLocal carrying the active [PolstTheme].
 *
 * Defaults to [PolstTheme.Companion.system] when no `PolstThemeProvider` is
 * present higher up in the composition.
 */
public val LocalPolstTheme: ProvidableCompositionLocal<PolstTheme> =
    staticCompositionLocalOf { PolstTheme.system() }
