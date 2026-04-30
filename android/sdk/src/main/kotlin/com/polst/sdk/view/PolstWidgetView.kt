package com.polst.sdk.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.res.use
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.polst.sdk.PolstClient
import com.polst.sdk.R
import com.polst.sdk.Vote
import com.polst.sdk.core.theme.PolstTheme
import com.polst.sdk.core.theme.PolstThemeProvider
import com.polst.sdk.core.theme.dark
import com.polst.sdk.core.theme.light
import com.polst.sdk.core.theme.system
import com.polst.sdk.network.PolstApiError
import com.polst.sdk.ui.PolstView
import com.polst.sdk.ui.locals.LocalPolstClient
import com.polst.sdk.ui.modifiers.PolstSize

/**
 * A classic Android [FrameLayout] that hosts the Compose-based [PolstView] widget.
 *
 * Use this from XML layouts or Java/Kotlin View-system codebases. Configure via XML attributes
 * under the `app:` namespace (polst_shortId, polst_theme, polst_accent, polst_size,
 * polst_hideTitle, polst_hideBrand) or via the public mutable properties.
 */
public class PolstWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Java-friendly SAM interface for vote callbacks. */
    public fun interface OnVoteListener {
        public fun onVote(vote: Vote)
    }

    /** Java-friendly SAM interface for error callbacks. */
    public fun interface OnErrorListener {
        public fun onError(error: PolstApiError)
    }

    private val composeView: ComposeView = ComposeView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    public var shortId: String? = null
        set(value) {
            field = value
            if (isAttachedToWindow) renderContent()
        }

    public var theme: PolstTheme? = null
        set(value) {
            field = value
            if (isAttachedToWindow) renderContent()
        }

    public var size: PolstSize = PolstSize.Standard
        set(value) {
            field = value
            if (isAttachedToWindow) renderContent()
        }

    public var hideTitle: Boolean = false
        set(value) {
            field = value
            if (isAttachedToWindow) renderContent()
        }

    public var hideBrand: Boolean = false
        set(value) {
            field = value
            if (isAttachedToWindow) renderContent()
        }

    public var accent: Color? = null
        set(value) {
            field = value
            if (isAttachedToWindow) renderContent()
        }

    private var voteListener: OnVoteListener? = null
    private var errorListener: OnErrorListener? = null

    public fun setOnVoteListener(listener: OnVoteListener?) {
        voteListener = listener
    }

    public fun setOnErrorListener(listener: OnErrorListener?) {
        errorListener = listener
    }

    init {
        addView(composeView)
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.PolstWidgetView, defStyleAttr, 0).use { ta ->
                shortId = ta.getString(R.styleable.PolstWidgetView_polst_shortId)

                val themeOrdinal = ta.getInt(R.styleable.PolstWidgetView_polst_theme, 0)
                theme = when (themeOrdinal) {
                    0 -> PolstTheme.system()
                    1 -> PolstTheme.light
                    2 -> PolstTheme.dark
                    else -> null
                }

                val sizeOrdinal = ta.getInt(R.styleable.PolstWidgetView_polst_size, 1)
                size = PolstSize.entries[sizeOrdinal.coerceIn(0, 2)]

                val accentArgb = ta.getColor(R.styleable.PolstWidgetView_polst_accent, 0)
                accent = if (accentArgb != 0) Color(accentArgb) else null

                hideTitle = ta.getBoolean(R.styleable.PolstWidgetView_polst_hideTitle, false)
                hideBrand = ta.getBoolean(R.styleable.PolstWidgetView_polst_hideBrand, false)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        renderContent()
    }

    private fun renderContent() {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        if (lifecycleOwner != null) {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycleOwner),
            )
        }
        composeView.setContent {
            val client = try {
                PolstClient.default
            } catch (_: Throwable) {
                null
            }
            if (client == null) {
                Text("Call PolstClient.installDefault(...) at app startup")
                return@setContent
            }
            PolstThemeProvider(theme = theme ?: PolstTheme.system()) {
                CompositionLocalProvider(LocalPolstClient provides client) {
                    val sid = shortId
                    if (sid.isNullOrBlank()) {
                        Text("Set polst_shortId attribute")
                    } else {
                        PolstView(
                            shortId = sid,
                            modifier = Modifier,
                            theme = theme,
                            onVote = { v -> voteListener?.onVote(v) },
                            onError = { e -> errorListener?.onError(e) },
                        )
                    }
                }
            }
        }
    }
}
