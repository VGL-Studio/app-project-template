package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the native ad components with a fake ad (Roborazzi), on the screen background with the 16 dp
 * screen margin, so the card's background and border show against the screen (KB-1). The placeholder is captured
 * with its animation off: an endless animation never lets the screen settle (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdCardScreenshotTest {
    @Composable
    private fun OnScreen(darkTheme: Boolean, content: @Composable () -> Unit) {
        AppTheme(darkTheme = darkTheme) {
            Surface {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) { content() }
            }
        }
    }

    @Test
    fun small_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_small_light.png") {
            OnScreen(darkTheme = false) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL) }
        }
    }

    @Test
    fun small_dark() {
        captureRoboImage("src/test/screenshots/NativeAdCard_small_dark.png") {
            OnScreen(darkTheme = true) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL) }
        }
    }

    @Test
    fun medium_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_medium_light.png") {
            OnScreen(darkTheme = false) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM) }
        }
    }

    @Test
    fun medium_dark() {
        captureRoboImage("src/test/screenshots/NativeAdCard_medium_dark.png") {
            OnScreen(darkTheme = true) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM) }
        }
    }

    @Test
    fun placeholder_medium_light() {
        captureRoboImage("src/test/screenshots/NativeAdPlaceholder_medium_light.png") {
            OnScreen(darkTheme = false) { NativeAdPlaceholder(NativeAdSize.MEDIUM, animated = false) }
        }
    }
}
