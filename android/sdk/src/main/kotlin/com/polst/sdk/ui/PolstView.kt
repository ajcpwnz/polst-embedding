package com.polst.sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.polst.sdk.Polst
import com.polst.sdk.PolstClient
import com.polst.sdk.PolstOption
import com.polst.sdk.Vote
import com.polst.sdk.core.theme.PolstTheme
import com.polst.sdk.network.PolstApiError
import com.polst.sdk.ui.locals.LocalPolstClient
import com.polst.sdk.ui.locals.LocalPolstTheme
import com.polst.sdk.ui.state.Freshness
import com.polst.sdk.ui.state.PolstViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Renders a single Polst identified by [shortId].
 *
 * The composable fetches the Polst via [PolstClient.polsts] on first
 * composition and whenever [shortId] changes, then renders the appropriate
 * state from the [PolstViewState] finite state machine. User taps on an
 * option submit a vote and emit the resulting [Vote] through [onVote]; API
 * failures are surfaced via [onError] and place the view in an
 * [PolstViewState.Error] state with a Retry affordance.
 *
 * When [theme] is `null`, the effective theme is sourced from
 * [LocalPolstTheme]. Callers can override per-widget by providing an explicit
 * [PolstTheme] — this is the MVP escape hatch; US6 wires deeper bridging.
 */
@Composable
public fun PolstView(
    shortId: String,
    modifier: Modifier = Modifier,
    client: PolstClient = LocalPolstClient.current ?: PolstClient.default,
    theme: PolstTheme? = null,
    onVote: (Vote) -> Unit = {},
    onError: (PolstApiError) -> Unit = {},
) {
    var state: PolstViewState by remember { mutableStateOf<PolstViewState>(PolstViewState.Idle) }
    var localVote: LocalVote? by remember { mutableStateOf<LocalVote?>(null) }
    var transientError: String? by remember { mutableStateOf<String?>(null) }
    var loadKey: Int by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val effectiveTheme: PolstTheme = theme ?: LocalPolstTheme.current

    LaunchedEffect(shortId, loadKey) {
        state = PolstViewState.Loading
        localVote = null
        transientError = null
        state = try {
            val result = client.polsts.getWithFreshness(shortId)
            val mapped = when (result.freshness) {
                com.polst.sdk.offline.CacheFreshness.Fresh -> Freshness.Fresh
                com.polst.sdk.offline.CacheFreshness.Stale -> Freshness.StaleStale
                com.polst.sdk.offline.CacheFreshness.StaleOffline -> Freshness.StaleOffline
            }
            PolstViewState.Loaded(result.polst, mapped)
        } catch (e: PolstApiError) {
            onError(e)
            PolstViewState.Error(e)
        }
    }

    LaunchedEffect(transientError) {
        val err = transientError
        if (err != null) {
            delay(4000)
            if (transientError == err) transientError = null
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = effectiveTheme.colors.background,
        contentColor = effectiveTheme.colors.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when (val current = state) {
                is PolstViewState.Idle, is PolstViewState.Loading -> LoadingContent(effectiveTheme)

                is PolstViewState.Loaded -> {
                    val visiblePolst: Polst = localVote?.polst ?: current.polst
                    val progressOptionId: String? = localVote?.optionId
                    PollContent(
                        polst = visiblePolst,
                        freshness = current.freshness,
                        theme = effectiveTheme,
                        selectedOptionId = progressOptionId,
                        votingOptionId = null,
                        enabled = localVote == null,
                        onOptionClick = { option ->
                            castOptimisticVote(
                                option = option,
                                polst = current.polst,
                                client = client,
                                shortId = shortId,
                                onVote = onVote,
                                onError = onError,
                                setLocalVote = { localVote = it },
                                setTransientError = { transientError = it },
                                launch = { scope.launch { it() } },
                            )
                        },
                    )
                }

                is PolstViewState.Voting, is PolstViewState.Voted -> Unit // legacy states; superseded by localVote

                is PolstViewState.Error -> ErrorContent(
                    theme = effectiveTheme,
                    onRetry = { loadKey += 1 },
                )
            }

            transientError?.let { msg ->
                Text(
                    text = msg,
                    color = effectiveTheme.colors.error,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = effectiveTheme.spacing.md, vertical = effectiveTheme.spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(theme: PolstTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(theme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = theme.colors.accent)
    }
}

@Composable
private fun ErrorContent(theme: PolstTheme, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(theme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Unable to load Polst.",
            style = TextStyle(
                fontSize = theme.typography.body.baseSize,
                fontWeight = theme.typography.body.weight,
                color = theme.colors.error,
            ),
        )
        Spacer(modifier = Modifier.height(theme.spacing.sm))
        OutlinedButton(onClick = onRetry) {
            Text(text = "Retry", color = theme.colors.accent)
        }
    }
}

private val ProgressFillColor: Color = Color(0xFF4F46E5)
private val ProgressBgColor: Color = Color(0xFFD1D5DB)

@Composable
private fun OptionColumn(
    option: PolstOption,
    theme: PolstTheme,
    enabled: Boolean,
    isLeft: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(enabled = enabled, role = Role.Button) { onClick() }
            .then(if (enabled) Modifier else Modifier.alpha(0.85f))
            .semantics {
                contentDescription = "Vote for ${option.label}"
                role = Role.Button
            },
    ) {
        Text(
            text = option.label,
            modifier = Modifier.padding(bottom = theme.spacing.xs),
            fontSize = theme.typography.body.baseSize,
            fontWeight = theme.typography.body.weight,
            color = theme.colors.onSurface,
        )
        if (!option.mediaUrl.isNullOrBlank()) {
            val imageShape = if (isLeft) {
                RoundedCornerShape(topStart = theme.radii.image, bottomStart = theme.radii.image)
            } else {
                RoundedCornerShape(topEnd = theme.radii.image, bottomEnd = theme.radii.image)
            }
            PolstAsyncImage(
                url = option.mediaUrl!!,
                contentDescription = option.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(imageShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun VoteProgressBar(
    polst: Polst,
    selectedOptionId: String,
    theme: PolstTheme,
    modifier: Modifier = Modifier,
) {
    val totalVotes: Long = polst.tallies.values.sum().coerceAtLeast(1L)
    val selectedVotes: Long = polst.tallies[selectedOptionId] ?: 0L
    val fillFraction: Float = (selectedVotes.toFloat() / totalVotes.toFloat()).coerceIn(0f, 1f)
    val percent: Int = (fillFraction * 100f).toInt().coerceIn(0, 100)
    val isLeftSelected: Boolean = polst.options.firstOrNull()?.id == selectedOptionId
    val selectedLabel: String = polst.options.firstOrNull { it.id == selectedOptionId }?.label.orEmpty()

    val fillSideTextColor: Color = Color.White
    val bgSideTextColor: Color = theme.colors.onSurface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(theme.radii.control))
            .background(ProgressBgColor)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = fillFraction)
                .align(if (isLeftSelected) Alignment.CenterStart else Alignment.CenterEnd)
                .background(ProgressFillColor),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = theme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLeftSelected) {
                Text(
                    text = "✓ $selectedLabel",
                    color = fillSideTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = "$percent%",
                    color = bgSideTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            } else {
                Text(
                    text = "$percent%",
                    color = bgSideTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = "$selectedLabel ✓",
                    color = fillSideTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun PollContent(
    polst: Polst,
    freshness: Freshness,
    theme: PolstTheme,
    selectedOptionId: String?,
    votingOptionId: String?,
    enabled: Boolean,
    onOptionClick: (PolstOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = theme.spacing.md, horizontal = theme.spacing.md)
            .semantics { contentDescription = "Polst poll: ${polst.question}" },
        verticalArrangement = Arrangement.spacedBy(theme.spacing.sm),
    ) {
        Text(
            text = polst.question,
            style = TextStyle(
                fontSize = theme.typography.title.baseSize,
                fontWeight = theme.typography.title.weight,
                color = theme.colors.onSurface,
            ),
        )

        val progressOptionId: String? = selectedOptionId ?: votingOptionId
        val hasVote: Boolean = progressOptionId != null

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (hasVote) 0.5f else 1f),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                polst.options.take(2).forEachIndexed { index, option ->
                    OptionColumn(
                        option = option,
                        theme = theme,
                        enabled = enabled && !hasVote,
                        isLeft = index == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { onOptionClick(option) },
                    )
                }
            }

            if (progressOptionId != null) {
                VoteProgressBar(
                    polst = polst,
                    selectedOptionId = progressOptionId,
                    theme = theme,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(theme.spacing.sm)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(theme.radii.control),
                            clip = false,
                        ),
                )
            }
        }

        if (freshness != Freshness.Fresh) {
            Text(
                text = "Showing cached results",
                style = TextStyle(
                    fontSize = theme.typography.caption.baseSize,
                    fontWeight = theme.typography.caption.weight,
                    color = theme.colors.border,
                ),
            )
        }
    }
}

private data class LocalVote(
    val optionId: String,
    val polst: Polst,
    val idempotencyKey: UUID,
)

private fun castOptimisticVote(
    option: PolstOption,
    polst: Polst,
    client: PolstClient,
    shortId: String,
    onVote: (Vote) -> Unit,
    onError: (PolstApiError) -> Unit,
    setLocalVote: (LocalVote?) -> Unit,
    setTransientError: (String?) -> Unit,
    launch: (suspend () -> Unit) -> Unit,
) {
    val idempotencyKey: UUID = UUID.randomUUID()
    val optimisticTallies: Map<String, Long> = buildMap {
        polst.tallies.forEach { (id, count) -> put(id, count) }
        put(option.id, (polst.tallies[option.id] ?: 0L) + 1L)
    }
    val optimisticPolst: Polst = polst.copy(tallies = optimisticTallies)
    setLocalVote(LocalVote(optionId = option.id, polst = optimisticPolst, idempotencyKey = idempotencyKey))
    setTransientError(null)
    launch {
        try {
            val vote: Vote = client.polsts.vote(shortId, option.id)
            onVote(vote)
            // Server-acknowledged. Deliberately do NOT touch local state — the optimistic
            // UI shown since the tap remains in place, preventing any flicker.
        } catch (e: PolstApiError) {
            onError(e)
            setLocalVote(null)
            setTransientError("Couldn't record your vote. Please try again.")
        }
    }
}
