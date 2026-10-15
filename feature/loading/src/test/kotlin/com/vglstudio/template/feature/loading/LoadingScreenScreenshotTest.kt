package com.vglstudio.template.feature.loading

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). Reference images live in src/test/screenshots and are compared on every CI run.
 * A changed reference image must be listed in the plan and reviewed by a person.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LoadingScreenScreenshotTest {
    @Test
    fun progress_light() {
        captureRoboImage("src/test/screenshots/LoadingScreen_progress_light.png") {
            AppTheme(darkTheme = false) { LoadingScreen(uiState = LoadingUiState.InProgress(0.5f), onRetry = {}) }
        }
    }

    @Test
    fun progress_dark() {
        captureRoboImage("src/test/screenshots/LoadingScreen_progress_dark.png") {
            AppTheme(darkTheme = true) { LoadingScreen(uiState = LoadingUiState.InProgress(0.5f), onRetry = {}) }
        }
    }

    @Test
    fun failed_light() {
        captureRoboImage("src/test/screenshots/LoadingScreen_failed_light.png") {
            AppTheme(darkTheme = false) { LoadingScreen(uiState = LoadingUiState.Failed(0.75f), onRetry = {}) }
        }
    }
}
