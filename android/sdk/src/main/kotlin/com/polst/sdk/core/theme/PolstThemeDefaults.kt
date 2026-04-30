package com.polst.sdk.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Default [PolstTheme] factories exposed as extensions on [PolstTheme.Companion].
 *
 * These are the Phase 2 base factories: `light`, `dark`, and `system()`.
 * `system()` currently returns [light] unconditionally; dynamic-color wiring
 * (Android 12+) is added in US6.
 *
 * Material 3 bridging and a `branded()` factory are intentionally omitted here
 * and land in later phases.
 */

private val DefaultRadii: PolstTheme.Radii = PolstTheme.Radii(
    card = 12.dp,
    control = 8.dp,
    image = 8.dp,
)

private val DefaultTypography: PolstTheme.Typography = PolstTheme.Typography(
    title = PolstTheme.TextRole(baseSize = 20.sp, weight = FontWeight.SemiBold),
    body = PolstTheme.TextRole(baseSize = 16.sp, weight = FontWeight.Normal),
    caption = PolstTheme.TextRole(baseSize = 12.sp, weight = FontWeight.Normal),
)

private val DefaultSpacing: PolstTheme.Spacing = PolstTheme.Spacing(
    xs = 4.dp,
    sm = 8.dp,
    md = 16.dp,
    lg = 24.dp,
    xl = 32.dp,
)

private val LightColors: PolstTheme.Colors = PolstTheme.Colors(
    accent = Color(0xFF6750A4),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    border = Color(0xFFE7E0EC),
    error = Color(0xFFB3261E),
    onAccent = Color(0xFFFFFFFF),
)

private val DarkColors: PolstTheme.Colors = PolstTheme.Colors(
    accent = Color(0xFFD0BCFF),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    border = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),
    onAccent = Color(0xFF371E73),
)

/** Default light [PolstTheme]. */
public val PolstTheme.Companion.light: PolstTheme
    get() = PolstTheme(
        colors = LightColors,
        radii = DefaultRadii,
        typography = DefaultTypography,
        spacing = DefaultSpacing,
    )

/** Default dark [PolstTheme]. */
public val PolstTheme.Companion.dark: PolstTheme
    get() = PolstTheme(
        colors = DarkColors,
        radii = DefaultRadii,
        typography = DefaultTypography,
        spacing = DefaultSpacing,
    )

/**
 * System-driven [PolstTheme] factory.
 *
 * In Phase 2 this returns [light] unconditionally. Dynamic-color resolution
 * (Android 12+ `android.R.color.system_*` tokens and light/dark detection) is
 * wired up in US6.
 */
public fun PolstTheme.Companion.system(): PolstTheme = light
