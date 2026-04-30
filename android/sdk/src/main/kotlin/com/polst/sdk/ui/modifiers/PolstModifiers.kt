package com.polst.sdk.ui.modifiers

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color

/**
 * Size variant applied by [polstSize]. The concrete visual impact (paddings,
 * typography scale, thumbnail sizes) is resolved by `PolstView` when it reads
 * [LocalPolstModifierConfig] from the surrounding composition.
 */
public enum class PolstSize {
    Compact,
    Standard,
    Large,
}

/**
 * Per-widget configuration surface driven by the public `Modifier` extensions
 * ([polstAccent], [polstSize], [hidePolstTitle], [hidePolstBrand]). Kept
 * internal so hosts cannot reach in and mutate the config directly.
 */
internal data class PolstModifierConfig(
    val accent: Color? = null,
    val size: PolstSize? = null,
    val hideTitle: Boolean = false,
    val hideBrand: Boolean = false,
)

/**
 * CompositionLocal holding the effective [PolstModifierConfig] for the
 * composition subtree. Defaults to a zero-override config so composables
 * without any `.polst*` modifiers behave identically to before.
 */
internal val LocalPolstModifierConfig = compositionLocalOf { PolstModifierConfig() }

/**
 * Overrides the accent color used by the nearest enclosing Polst composable.
 *
 * TODO US1 polish: wire config through to PolstView.
 */
public fun Modifier.polstAccent(color: Color): Modifier = composed {
    // TODO US1 polish: wire config through to PolstView.
    this
}

/**
 * Selects a size variant for the nearest enclosing Polst composable.
 *
 * TODO US1 polish: wire config through to PolstView.
 */
public fun Modifier.polstSize(size: PolstSize): Modifier = composed {
    // TODO US1 polish: wire config through to PolstView.
    this
}

/**
 * Hides the question/title text of the nearest enclosing Polst composable.
 *
 * TODO US1 polish: wire config through to PolstView.
 */
public fun Modifier.hidePolstTitle(): Modifier = composed {
    // TODO US1 polish: wire config through to PolstView.
    this
}

/**
 * Hides the brand attribution row of the nearest enclosing Polst composable.
 *
 * TODO US1 polish: wire config through to PolstView.
 */
public fun Modifier.hidePolstBrand(): Modifier = composed {
    // TODO US1 polish: wire config through to PolstView.
    this
}
