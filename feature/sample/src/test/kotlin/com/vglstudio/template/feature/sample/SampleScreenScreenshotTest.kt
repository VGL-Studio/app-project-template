package com.vglstudio.template.feature.sample

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.template.core.ads.BottomAdBarDefaults
import com.vglstudio.template.core.ads.BottomAdBarLayout
import com.vglstudio.template.core.ads.CollapsibleNativeAdCard
import com.vglstudio.template.core.ads.NativeAdAssets
import com.vglstudio.template.core.ads.NativeAdCard
import com.vglstudio.template.core.ads.NativeAdPlaceholder
import com.vglstudio.template.core.ads.NativeAdSize
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). Reference images live in src/test/screenshots and are compared on every CI run.
 * A changed reference image must be listed in the plan and reviewed by a person.
 * Tests have no ads controller, so the bottom bar draws no ad: the bar is [BottomAdBarLayout] with a fake
 * [NativeAdCard] passed through `bottomBar`, with the illustration texts of the design (docs/features/ads/design/).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SampleScreenScreenshotTest {
    @Test
    fun ready_light() {
        captureRoboImage("src/test/screenshots/SampleScreen_ready_light.png") {
            AppTheme(darkTheme = false) {
                SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}, bottomBar = fakeBar())
            }
        }
    }

    @Test
    fun ready_dark() {
        captureRoboImage("src/test/screenshots/SampleScreen_ready_dark.png") {
            AppTheme(darkTheme = true) {
                SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}, bottomBar = fakeBar())
            }
        }
    }

    @Test
    fun ad_loading_light() {
        captureRoboImage("src/test/screenshots/SampleScreen_ad-loading_light.png") {
            AppTheme(darkTheme = false) {
                SampleScreen(
                    uiState = SampleUiState.Ready(count = 3),
                    onIncrement = {},
                    bottomBar = { content ->
                        // The placeholder of the first load; the screenshot keeps the space of the slot, as before.
                        BottomAdBarLayout(
                            reservation = BottomAdBarDefaults.nativeReservation,
                            slot = { NativeAdPlaceholder(NativeAdSize.SMALL, it, animated = false) },
                            layer = null,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun ready_small() {
        captureRoboImage("src/test/screenshots/SampleScreen_ready_small.png") {
            AppTheme(darkTheme = false) {
                SampleScreen(
                    uiState = SampleUiState.Ready(count = 3, saveFailed = true),
                    onIncrement = {},
                    bottomBar = fakeBar(),
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun bar_expanded_small() {
        // L-028: the open 250 dp collapsible layer on the low frame does not cover the main button.
        captureRoboImage("src/test/screenshots/SampleScreen_bar-expanded_small.png") {
            AppTheme(darkTheme = false) {
                SampleScreen(
                    uiState = SampleUiState.Ready(count = 3, saveFailed = true),
                    onIncrement = {},
                    bottomBar = fakeBar(layer = true),
                )
            }
        }
    }

    @Test
    fun disabled_light() {
        captureRoboImage("src/test/screenshots/SampleScreen_disabled_light.png") {
            AppTheme(darkTheme = false) { SampleScreen(uiState = SampleUiState.Disabled, onIncrement = {}) }
        }
    }

    /** The bottom bar with fake ads: the slot's native small card, and with [layer] the open collapsible layer. */
    private fun fakeBar(layer: Boolean = false): @Composable (@Composable (PaddingValues) -> Unit) -> Unit =
        { content ->
            BottomAdBarLayout(
                reservation = BottomAdBarDefaults.nativeReservation,
                slot = { NativeAdCard(assets = adAssets, size = NativeAdSize.SMALL, modifier = it) },
                layer = if (layer) {
                    { CollapsibleNativeAdCard(assets = adAssets.copy(body = null), onCollapse = {}, modifier = it) }
                } else {
                    null
                },
                content = content,
            )
        }

    private val adAssets = NativeAdAssets(
        headline = "Headline of the ad",
        advertiser = "Advertiser or short body",
        body = "Body text of the ad, two lines at most so the call to action stays in view.",
        callToAction = "Install",
        icon = null,
        hasMedia = true,
    )
}
