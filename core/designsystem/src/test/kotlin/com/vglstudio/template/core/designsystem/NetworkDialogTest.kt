package com.vglstudio.template.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.component.NetworkDialogScreen
import com.vglstudio.template.core.designsystem.component.NetworkDialogTestTags
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The network dialog on the JVM (spec network, KB-2; design network, text table and sizes). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NetworkDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setDialog(checking: Boolean = false, stillOffline: Boolean = false, onRetry: () -> Unit = {}) {
        composeRule.setContent {
            AppTheme {
                NetworkDialogScreen(
                    title = TITLE,
                    message = MESSAGE,
                    retryLabel = RETRY,
                    stillOfflineMessage = if (stillOffline) STILL_OFFLINE else null,
                    checking = checking,
                    onRetry = onRetry,
                )
            }
        }
    }

    @Test
    fun default_showsTextAndIcon() {
        setDialog()

        composeRule.onAllNodesWithText(TITLE).assertCountEquals(1)
        composeRule.onAllNodesWithText(MESSAGE).assertCountEquals(1)
        composeRule.onAllNodesWithText(RETRY, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag(NetworkDialogTestTags.ICON, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag(NetworkDialogTestTags.STILL_OFFLINE).assertCountEquals(0)
        composeRule.onAllNodesWithText(STILL_OFFLINE).assertCountEquals(0)
    }

    @Test
    fun card_is312dpWide_andRetry264dp() {
        setDialog()

        composeRule.onNodeWithTag(NetworkDialogTestTags.CARD).assertWidthIsEqualTo(312.dp)
        val retry = composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).getUnclippedBoundsInRoot()
        assertTrue("height ${retry.height}", retry.height >= 52.dp)
        val expectedWidth = 312.dp - Spacing.lg * 2
        assertEquals(264f, expectedWidth.value, 0f)
        assertEquals(expectedWidth.value, retry.width.value, 0.5f)
    }

    @Test
    fun screen_consumesTouches() {
        var behind = 0
        var retries = 0
        composeRule.setContent {
            AppTheme {
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { behind++ }, modifier = Modifier.testTag(BEHIND)) { Text("Behind") }
                    NetworkDialogScreen(
                        title = TITLE,
                        message = MESSAGE,
                        retryLabel = RETRY,
                        stillOfflineMessage = null,
                        checking = false,
                        onRetry = { retries++ },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(BEHIND).performClick()
        composeRule.onNodeWithTag(NetworkDialogTestTags.SCREEN).performTouchInput { click(topLeft + Offset(10f, 10f)) }

        assertEquals(0, behind)
        assertEquals(0, retries)
        composeRule.onNodeWithTag(NetworkDialogTestTags.CARD).assertExists()
    }

    @Test
    fun retry_click_callsOnRetry() {
        var retries = 0
        setDialog(onRetry = { retries++ })

        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun checking_showsSpinner_andIgnoresClicks() {
        var retries = 0
        setDialog(checking = true, onRetry = { retries++ })

        composeRule.onNodeWithTag(NetworkDialogTestTags.SPINNER, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).performClick()

        assertEquals(0, retries)
    }

    @Test
    fun stillOffline_showsMessageAboveButton() {
        setDialog(stillOffline = true)

        composeRule.onAllNodesWithText(STILL_OFFLINE).assertCountEquals(1)
        val message = composeRule.onNodeWithTag(NetworkDialogTestTags.STILL_OFFLINE).getUnclippedBoundsInRoot()
        val retry = composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).getUnclippedBoundsInRoot()
        assertTrue("message bottom ${message.bottom}, retry top ${retry.top}", message.bottom <= retry.top)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun smallScreen_cardFitsAndRetryVisible() {
        setDialog(stillOffline = true)

        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(NetworkDialogTestTags.CARD).getUnclippedBoundsInRoot()
        assertTrue("card $card in $screen", card.left >= screen.left && card.right <= screen.right)
        assertTrue("card $card in $screen", card.top >= screen.top && card.bottom <= screen.bottom)
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).assertIsDisplayed()
    }

    private companion object {
        // Text table of the design (design network, section 2) and the line after a failed retry (plan T-017).
        const val TITLE = "Không có kết nối mạng"
        const val MESSAGE = "Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại."
        const val RETRY = "Thử lại"
        const val STILL_OFFLINE = "Vẫn chưa có kết nối. Hãy thử lại sau ít phút."
        const val BEHIND = "behind"
    }
}
