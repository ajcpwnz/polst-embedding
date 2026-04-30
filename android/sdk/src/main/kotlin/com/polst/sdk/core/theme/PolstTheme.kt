package com.polst.sdk.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * Immutable theme token container for the Polst SDK's Compose surface area.
 *
 * The theme is split into four token groups: [Colors], [Radii], [Typography] and
 * [Spacing]. Hosts typically obtain an instance through the factory helpers on
 * [PolstTheme.Companion] (see `PolstThemeDefaults.kt`) and provide it through
 * `PolstThemeProvider`.
 *
 * This file intentionally has no dependency on Material 3 or any Android
 * Context, so it can be safely referenced from pure-Compose modules.
 */
public data class PolstTheme(
    public val colors: Colors,
    public val radii: Radii,
    public val typography: Typography,
    public val spacing: Spacing,
) {
    public data class Colors(
        public val accent: Color,
        public val background: Color,
        public val surface: Color,
        public val onSurface: Color,
        public val border: Color,
        public val error: Color,
        public val onAccent: Color,
    )

    public data class Radii(
        public val card: Dp,
        public val control: Dp,
        public val image: Dp,
    )

    public data class TextRole(
        public val baseSize: TextUnit,
        public val weight: FontWeight,
    )

    public data class Typography(
        public val title: TextRole,
        public val body: TextRole,
        public val caption: TextRole,
    )

    public data class Spacing(
        public val xs: Dp,
        public val sm: Dp,
        public val md: Dp,
        public val lg: Dp,
        public val xl: Dp,
    )

    public companion object
}
