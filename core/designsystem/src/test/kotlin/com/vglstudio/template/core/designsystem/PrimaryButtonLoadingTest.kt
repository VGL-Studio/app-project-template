package com.vglstudio.template.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.component.PrimaryButton
import com.vglstudio.template.core.designsystem.component.PrimaryButtonTestTags
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The waiting state of [PrimaryButton], used by "Retry" of the network dialog (spec network, KB-2). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrimaryButtonLoadingTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_showsSpinner_keepsText_ignoresClick() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                PrimaryButton(
                    text = "Retry",
                    onClick = { clicks++ },
                    modifier = Modifier.testTag(BUTTON),
                    loading = true,
                )
            }
        }

        composeRule.onNodeWithTag(PrimaryButtonTestTags.SPINNER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Retry", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(BUTTON).performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun notLoading_hasNoSpinner() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                Column {
                    PrimaryButton(text = "Retry", onClick = { clicks++ }, modifier = Modifier.testTag(BUTTON))
                    // Control next to it: the same button while loading has a spinner, so the check below can fail.
                    PrimaryButton(text = "Wait", onClick = {}, modifier = Modifier.testTag(LOADING), loading = true)
                }
            }
        }

        composeRule.onAllNodes(spinnerIn(BUTTON), useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodes(spinnerIn(LOADING), useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNodeWithTag(BUTTON).performClick()
        assertEquals(1, clicks)
    }

    private fun spinnerIn(button: String) =
        hasTestTag(PrimaryButtonTestTags.SPINNER) and hasAnyAncestor(hasTestTag(button))

    private companion object {
        const val BUTTON = "button"
        const val LOADING = "loading"
    }
}
