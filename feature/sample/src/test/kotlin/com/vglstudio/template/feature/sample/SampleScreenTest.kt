package com.vglstudio.template.feature.sample

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
class SampleScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun whenReady_thenShowsCount() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 7), onIncrement = {}) }
        }
        composeRule.onNodeWithTag(SampleTestTags.COUNT).assertTextEquals("7")
    }

    @Test
    fun whenTapButton_thenCallsOnIncrement() {
        var taps = 0
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Ready(count = 0), onIncrement = { taps++ }) }
        }
        composeRule.onNodeWithText(string(R.string.sample_increment)).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun whenDisabled_thenShowsNotAvailableMessage() {
        composeRule.setContent {
            AppTheme { SampleScreen(uiState = SampleUiState.Disabled, onIncrement = {}) }
        }
        composeRule.onNodeWithText(string(R.string.sample_disabled)).assertIsDisplayed()
    }
}
