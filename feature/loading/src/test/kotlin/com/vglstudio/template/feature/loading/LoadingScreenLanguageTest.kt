package com.vglstudio.template.feature.loading

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.theme.AppTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Text of the loading screen in each language of the template: `res/values/` is English, `res/values-vi/` is
 * Vietnamese. `fr` stands for any language without its own folder, so it proves `values/` itself is English.
 *
 * Runs only in the template repository (it has `scripts/new-app.sh`, which `new-app.sh` deletes in an app): in an app
 * the whole class is reported as skipped, since an app replaces this text with its own and must not be held to the
 * text of the template.
 *
 * The test reads `scripts/new-app.sh` outside the inputs Gradle knows, so Gradle may consider the test task up to date
 * when only that file changes: run it with `--rerun` when checking.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoadingScreenLanguageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun onlyInTemplate() {
        assumeTrue("Not the template repository", File(findRepoRoot(), "scripts/new-app.sh").isFile)
    }

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun progress_otherLocale_showsEnglish() {
        setScreen(LoadingUiState.InProgress(PROGRESS))

        composeRule.onNodeWithText("Loading data").assertIsDisplayed()
        composeRule.onNodeWithText("Preparing your space…").assertIsDisplayed()
        composeRule.onNodeWithText("A private space for your work").assertIsDisplayed()
        composeRule.onNodeWithText("Everything in rhythm.").assertIsDisplayed()
        composeRule.onNodeWithText("68%").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun failed_otherLocale_showsEnglish() {
        setScreen(LoadingUiState.Failed(PROGRESS))

        composeRule.onNodeWithText("Progress stopped").assertIsDisplayed()
        composeRule.onNodeWithText("Unable to load data").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The connection was interrupted. Check your network and try again to continue.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Try again", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("68%").assertIsDisplayed()
        composeRule.onNodeWithText("Preparing your space…").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
    fun progress_vietnamese_showsVietnamese() {
        setScreen(LoadingUiState.InProgress(PROGRESS))

        composeRule.onNodeWithText("Đang tải dữ liệu").assertIsDisplayed()
        composeRule.onNodeWithText("Đang chuẩn bị không gian của bạn…").assertIsDisplayed()
        composeRule.onNodeWithText("Không gian riêng cho việc của bạn").assertIsDisplayed()
        composeRule.onNodeWithText("Mọi việc vào đúng nhịp.").assertIsDisplayed()
        composeRule.onNodeWithText("68%").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
    fun failed_vietnamese_showsVietnamese() {
        setScreen(LoadingUiState.Failed(PROGRESS))

        composeRule.onNodeWithText("Tiến độ đã dừng").assertIsDisplayed()
        composeRule.onNodeWithText("Không thể tải dữ liệu").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Thử lại", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("68%").assertIsDisplayed()
        composeRule.onNodeWithText("Đang chuẩn bị không gian của bạn…").assertDoesNotExist()
    }

    // Same checks as LoadingScreenTest.failed_onShortScreen_retryButtonFitsOnScreen, with the Vietnamese text.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun failed_onShortScreen_vietnamese_retryButtonFits() {
        var retries = 0
        composeRule.setContent {
            AppTheme {
                LoadingScreen(uiState = LoadingUiState.Failed(PROGRESS), appName = APP_NAME, onRetry = { retries++ })
            }
        }
        // The layout below is measured with the Vietnamese text.
        composeRule.onNodeWithText("Thử lại", useUnmergedTree = true).assertExists()
        val footer = composeRule.onNodeWithText("Không gian riêng cho việc của bạn")
        footer.assertExists()

        val screenBottom = composeRule.onNodeWithTag(LoadingTestTags.SCREEN).getBoundsInRoot().bottom
        val retryBottom = composeRule.onNodeWithTag(LoadingTestTags.RETRY).getBoundsInRoot().bottom
        assertTrue("retry bottom $retryBottom is below screen bottom $screenBottom", retryBottom <= screenBottom)
        val footerTop = footer.getBoundsInRoot().top
        assertTrue("retry bottom $retryBottom is under footer top $footerTop", retryBottom <= footerTop)

        composeRule.onNodeWithTag(LoadingTestTags.RETRY).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(uiState: LoadingUiState) {
        composeRule.setContent {
            AppTheme { LoadingScreen(uiState = uiState, appName = APP_NAME, onRetry = {}) }
        }
    }

    private fun findRepoRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").isFile) dir = dir.parentFile
        return checkNotNull(dir) { "No settings.gradle.kts above ${File("").absolutePath}" }
    }

    private companion object {
        const val APP_NAME = "Todo App"
        const val PROGRESS = 0.68f
    }
}
