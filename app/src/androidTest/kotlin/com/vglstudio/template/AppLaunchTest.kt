package com.vglstudio.template

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smoke test on a real emulator: the app starts on the loading screen and moves to the start screen. Runs in CI only. */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStarts_showsLoadingThenStartScreen() {
        composeRule.onNodeWithTag("loading_screen").assertIsDisplayed()
        // The loading screen downloads files from staging R2 and waits at least 2 seconds.
        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithTag("sample_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("sample_screen").assertIsDisplayed()
    }
}
