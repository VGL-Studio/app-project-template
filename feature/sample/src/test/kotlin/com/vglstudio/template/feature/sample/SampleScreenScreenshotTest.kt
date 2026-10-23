package com.vglstudio.template.feature.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
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
 * Tests have no ads controller, so the native ad slot draws nothing: the ad is a fake [NativeAdCard] passed through
 * `bottomAd`, with the illustration texts of the design (docs/features/ads/design/).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SampleScreenScreenshotTest {
    @Test
    fun ready_light() {
        captureRoboImage("src/test/screenshots/SampleScreen_ready_light.png") {
            AppTheme(darkTheme = false) {
                SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}, bottomAd = fakeAd)
            }
        }
    }

    @Test
    fun ready_dark() {
        captureRoboImage("src/test/screenshots/SampleScreen_ready_dark.png") {
            AppTheme(darkTheme = true) {
                SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {}, bottomAd = fakeAd)
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
                    bottomAd = { NativeAdPlaceholder(NativeAdSize.SMALL, it, animated = false) },
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
                    bottomAd = fakeAd,
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

    private val fakeAd: @Composable (Modifier) -> Unit = { modifier ->
        NativeAdCard(
            assets = NativeAdAssets(
                headline = "Headline of the ad",
                advertiser = "Advertiser or short body",
                body = "Body text of the ad, two lines at most so the call to action stays in view.",
                callToAction = "Install",
                icon = null,
                hasMedia = true,
            ),
            size = NativeAdSize.SMALL,
            modifier = modifier,
        )
    }
}
