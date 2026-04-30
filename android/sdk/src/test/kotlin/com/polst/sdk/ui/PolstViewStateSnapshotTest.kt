// Phase 3 snapshots render the Loaded-state shape directly. A full PolstView snapshot pipeline
// lands in Phase 6 once we have FakePolstClient in :sdk-test.
package com.polst.sdk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.polst.sdk.BrandRef
import com.polst.sdk.Polst
import com.polst.sdk.PolstOption
import com.polst.sdk.core.theme.PolstTheme
import com.polst.sdk.core.theme.light
import org.junit.Rule
import org.junit.Test
import java.time.Instant

public class PolstViewStateSnapshotTest {

    @get:Rule
    public val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private val polst: Polst = Polst(
        shortId = "abc",
        question = "What's your favorite?",
        options = listOf(
            PolstOption("opt-a", "Apples", null),
            PolstOption("opt-b", "Bananas", null),
        ),
        tallies = mapOf("opt-a" to 42L, "opt-b" to 17L),
        brand = BrandRef("acme", "Acme", null),
        themingHints = null,
        media = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        version = 1,
    )

    @Test
    public fun loading_state() {
        val theme = PolstTheme.light
        paparazzi.snapshot(name = "loading_state") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.colors.background,
                contentColor = theme.colors.onSurface,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(theme.spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = theme.colors.accent)
                        Spacer(modifier = Modifier.height(theme.spacing.md))
                        Text(
                            text = "Loading…",
                            color = theme.colors.onSurface,
                            fontSize = theme.typography.body.baseSize,
                        )
                    }
                }
            }
        }
    }

    @Test
    public fun error_state_notFound() {
        val theme = PolstTheme.light
        paparazzi.snapshot(name = "error_state_notFound") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.colors.background,
                contentColor = theme.colors.onSurface,
            ) {
                Column(modifier = Modifier.padding(theme.spacing.md)) {
                    Text(
                        text = "Polst not found",
                        color = theme.colors.error,
                        fontSize = theme.typography.title.baseSize,
                        fontWeight = theme.typography.title.weight,
                    )
                    Spacer(modifier = Modifier.height(theme.spacing.md))
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.colors.accent,
                            contentColor = theme.colors.onAccent,
                        ),
                    ) {
                        Text(text = "Retry")
                    }
                }
            }
        }
    }

    @Test
    public fun voted_state() {
        val theme = PolstTheme.light
        val votedOptionId = "opt-a"
        paparazzi.snapshot(name = "voted_state") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = theme.colors.background,
                contentColor = theme.colors.onSurface,
            ) {
                Column(modifier = Modifier.padding(theme.spacing.md)) {
                    Text(
                        text = polst.question,
                        fontSize = theme.typography.title.baseSize,
                        fontWeight = theme.typography.title.weight,
                        color = theme.colors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(theme.spacing.md))
                    polst.options.forEach { option ->
                        val label = if (option.id == votedOptionId) {
                            "✓ Voted: ${option.label}"
                        } else {
                            option.label
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = theme.spacing.sm),
                            enabled = option.id != votedOptionId,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.colors.accent,
                                contentColor = theme.colors.onAccent,
                            ),
                        ) {
                            Text(text = label)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        polst.options.forEach { option ->
                            val count = polst.tallies[option.id] ?: 0L
                            Text(
                                text = "${option.label}: $count",
                                fontSize = theme.typography.caption.baseSize,
                                color = theme.colors.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
