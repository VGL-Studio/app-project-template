package com.vglstudio.template.network

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.template.R
import com.vglstudio.template.core.designsystem.component.NetworkDialogScreen
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.feature.sample.SampleScreen
import com.vglstudio.template.feature.sample.SampleUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The network dialog over the sample screen, as in the design frames `NetworkLostScreen_*` (ADR-0020). Drawn in one
 * window so onRoot() captures the dialog and its scrim; the real dialog window is checked in NetworkDialogHostTest.
 * Vietnamese is the default language of the app, so the frames pin the `vi` locale (Robolectric starts in English).
 * Nothing animates in these states (no spinner), so the screen settles (L-048). The native ad slot stays empty:
 * there is no ads controller in tests.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class NetworkLostScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(darkTheme: Boolean, stillOffline: Boolean) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize()) {
                    SampleScreen(uiState = SampleUiState.Ready(count = 3), onIncrement = {})
                    NetworkDialogScreen(
                        title = stringResource(R.string.network_lost_title),
                        message = stringResource(R.string.network_lost_message),
                        retryLabel = stringResource(R.string.network_lost_retry),
                        stillOfflineMessage = if (stillOffline) {
                            stringResource(
                                R.string.network_lost_still_offline,
                            )
                        } else {
                            null
                        },
                        checking = false,
                        onRetry = {},
                    )
                }
            }
        }
    }

    @Test
    fun default_light() {
        setScreen(darkTheme = false, stillOffline = false)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/NetworkLostScreen_default_light.png")
    }

    @Test
    fun default_dark() {
        setScreen(darkTheme = true, stillOffline = false)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/NetworkLostScreen_default_dark.png")
    }

    @Test
    fun stillOffline_light() {
        setScreen(darkTheme = false, stillOffline = true)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/NetworkLostScreen_stillOffline_light.png")
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun stillOffline_small() {
        setScreen(darkTheme = false, stillOffline = true)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/NetworkLostScreen_stillOffline_small.png")
    }
}
