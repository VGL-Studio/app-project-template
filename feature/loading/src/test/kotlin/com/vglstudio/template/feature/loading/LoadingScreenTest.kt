package com.vglstudio.template.feature.loading

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI behaviour of the stateless screen, run on the JVM with Robolectric (no emulator needed). */
@RunWith(AndroidJUnit4::class)
class LoadingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun inProgress_showsProgressBarPercentAndMessage() {
        composeRule.setContent {
            AppTheme { LoadingScreen(uiState = LoadingUiState.InProgress(0.5f), onRetry = {}) }
        }
        composeRule.onNodeWithTag(LoadingTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(LoadingTestTags.PROGRESS).assertIsDisplayed()
        composeRule.onNodeWithTag(LoadingTestTags.PERCENT).assertTextEquals("50%")
        composeRule.onNodeWithText(string(R.string.loading_message)).assertIsDisplayed()
        composeRule.onNodeWithTag(LoadingTestTags.ERROR).assertDoesNotExist()
    }

    @Test
    fun failed_showsErrorAndRetry_andRetryCallsOnRetry() {
        var retries = 0
        composeRule.setContent {
            AppTheme { LoadingScreen(uiState = LoadingUiState.Failed(0.75f), onRetry = { retries++ }) }
        }
        composeRule.onNodeWithTag(LoadingTestTags.PERCENT).assertTextEquals("75%")
        composeRule.onNodeWithTag(LoadingTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.loading_retry)).performClick()
        assertEquals(1, retries)
    }
}
