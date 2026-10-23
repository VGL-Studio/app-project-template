package com.vglstudio.template.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 * Screenshots of the collapsible native ad with a fake ad (Roborazzi), at the bottom of the screen with the 16 dp
 * screen margin, as in the design frames. The design-named images `AdGalleryScreen_collapsible-*` belong to the
 * gallery screen (T-014); these have their own names. Nothing here animates, so the screen settles (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CollapsibleNativeAdScreenshotTest {
    @Composable
    private fun AtBottom(darkTheme: Boolean, collapsed: Boolean) {
        AppTheme(darkTheme = darkTheme) {
            Surface {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    CollapsibleNativeAdCard(
                        assets = screenshotAd,
                        collapsed = collapsed,
                        onCollapse = {},
                        modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
                    )
                }
            }
        }
    }

    @Test
    fun expanded_light() {
        captureRoboImage("src/test/screenshots/CollapsibleNativeAd_expanded_light.png") {
            AtBottom(darkTheme = false, collapsed = false)
        }
    }

    @Test
    fun collapsed_light() {
        captureRoboImage("src/test/screenshots/CollapsibleNativeAd_collapsed_light.png") {
            AtBottom(darkTheme = false, collapsed = true)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun expanded_small() {
        captureRoboImage("src/test/screenshots/CollapsibleNativeAd_expanded_small.png") {
            AtBottom(darkTheme = false, collapsed = false)
        }
    }

    @Test
    fun expanded_dark() {
        captureRoboImage("src/test/screenshots/CollapsibleNativeAd_expanded_dark.png") {
            AtBottom(darkTheme = true, collapsed = false)
        }
    }

    @Test
    fun collapsed_dark() {
        captureRoboImage("src/test/screenshots/CollapsibleNativeAd_collapsed_dark.png") {
            AtBottom(darkTheme = true, collapsed = true)
        }
    }

    private companion object {
        /** The illustration texts of the collapsible frames (design/context/). */
        val screenshotAd = NativeAdAssets(
            headline = "Headline of the ad",
            advertiser = "Advertiser",
            body = null,
            callToAction = "Install",
            icon = null,
            hasMedia = true,
        )
    }
}
