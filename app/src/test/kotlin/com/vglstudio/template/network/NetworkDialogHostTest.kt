package com.vglstudio.template.network

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.component.NetworkDialogTestTags
import com.vglstudio.template.core.network.NetworkDialogState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * The shell that draws the network dialog over every screen (spec network, KB-2). robolectric.properties uses a
 * plain Application, so neither Hilt nor Firebase starts. Back is sent to the dialog's own dispatcher: Robolectric
 * does not route the activity's Back into the dialog window (plan T-017, section 4.1). No Espresso.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NetworkDialogHostTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContentButton(state: NetworkDialogState, onBack: () -> Unit = {}, onClick: () -> Unit = {}) {
        composeRule.setContent {
            NetworkDialogHost(state = state, onRetry = {}, onBack = onBack) {
                Button(onClick = onClick, modifier = Modifier.testTag(CONTENT)) { Text("Content") }
            }
        }
        composeRule.waitForIdle()
    }

    private fun latestDialog(): ComponentDialog {
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull("no dialog window was opened", dialog)
        return dialog as ComponentDialog
    }

    @Test
    fun shown_drawsDialogInSeparateWindow() {
        setContentButton(SHOWN)

        // A separate top window covers the app window, so the system keeps touches and screen readers off the
        // content behind it. Robolectric does not model that blocking; it is checked on a real device.
        val roots = composeRule.onAllNodes(isRoot())
        roots.assertCountEquals(2)
        roots[0].assert(hasAnyDescendant(hasTestTag(CONTENT)))
        roots[1].assert(hasAnyDescendant(hasTestTag(NetworkDialogTestTags.SCREEN)))
        // Still in the semantics tree, so AppLaunchTest finds the screen behind the dialog.
        assertEquals(1, composeRule.onAllNodesWithTag(CONTENT).fetchSemanticsNodes().size)
    }

    @Test
    fun shown_dialogWindowAddsNoSystemDim() {
        setContentButton(SHOWN)

        // The dialog draws its own 32% scrim (design); a system dim behind it would darken the screen twice.
        val window = latestDialog().window!!
        val dims = window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0 &&
            window.attributes.dimAmount > 0f
        assertFalse(dims)
    }

    @Test
    @Config(qualifiers = "vi")
    fun shown_usesAppStrings_vi() {
        setContentButton(SHOWN)

        composeRule.onAllNodesWithText("Không có kết nối mạng").assertCountEquals(1)
        composeRule.onAllNodesWithText(
            "Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại.",
        ).assertCountEquals(1)
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).assertTextEquals("Thử lại")
    }

    @Test
    @Config(qualifiers = "en")
    fun shown_usesAppStrings_en() {
        setContentButton(NetworkDialogState.Shown(checking = false, stillOffline = true))

        composeRule.onAllNodesWithText("No internet connection").assertCountEquals(1)
        composeRule.onAllNodesWithText(
            "This app needs the Internet to work. Check Wi-Fi or mobile data, then try again.",
        ).assertCountEquals(1)
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).assertTextEquals("Try again")
        composeRule.onAllNodesWithText("Still no connection. Please try again in a moment.").assertCountEquals(1)
    }

    @Test
    fun back_onDialogWindow_keepsDialogAndDestination() {
        var backs = 0
        setNavContent({ SHOWN }) { backs++ }

        // System Back goes to the top window, which is the dialog while it is shown.
        val dialog = latestDialog()
        composeRule.runOnUiThread { dialog.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(1, backs)
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithTag(SECOND).assertExists()
        composeRule.onNodeWithTag(NetworkDialogTestTags.SCREEN).assertExists()
    }

    @Test
    fun dialogDispatcher_isTheOneComposeSeesInsideTheDialog() {
        setContentButton(SHOWN)

        // BackHandler inside the dialog content registers on the dispatcher Compose sees there
        // (LocalOnBackPressedDispatcherOwner): it is the ComponentDialog's, not the activity's.
        val dialogDispatcher = latestDialog().onBackPressedDispatcher
        val activityDispatcher = composeRule.activity.onBackPressedDispatcher
        assertNotSame(activityDispatcher, dialogDispatcher)
        assertTrue(dialogDispatcher.hasEnabledCallbacks())
        assertFalse(activityDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun back_whileHidden_navigatesBack() {
        var backs = 0
        var state: NetworkDialogState by mutableStateOf(NetworkDialogState.Hidden)
        setNavContent({ state }) { backs++ }

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(0, backs)
        composeRule.onNodeWithTag(FIRST).assertExists()
        assertTrue(composeRule.onAllNodesWithTag(SECOND).fetchSemanticsNodes().isEmpty())

        // Control: the same host does open the dialog once shown, so the silence above comes from Hidden.
        state = SHOWN
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NetworkDialogTestTags.SCREEN).assertExists()
    }

    @Test
    fun hidingDialog_keepsContentState() {
        var state: NetworkDialogState by mutableStateOf(NetworkDialogState.Hidden)
        composeRule.setContent {
            NetworkDialogHost(state = state, onRetry = {}, onBack = {}) {
                var count by rememberSaveable { mutableStateOf(0) }
                Button(onClick = { count++ }, modifier = Modifier.testTag(CONTENT)) { Text("Count $count") }
            }
        }
        repeat(3) { composeRule.onNodeWithTag(CONTENT).performClick() }
        composeRule.onNodeWithTag(CONTENT).assertTextEquals("Count 3")

        state = SHOWN
        composeRule.waitForIdle()
        // Control: the dialog really opened over the content.
        composeRule.onNodeWithTag(NetworkDialogTestTags.SCREEN).assertExists()

        state = NetworkDialogState.Hidden
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(NetworkDialogTestTags.SCREEN).assertCountEquals(0)
        composeRule.onNodeWithTag(CONTENT).assertTextEquals("Count 3")
    }

    /** A two-destination NavHost already on its second destination. */
    private fun setNavContent(state: () -> NetworkDialogState, onBack: () -> Unit) {
        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            NetworkDialogHost(state = state(), onRetry = {}, onBack = onBack) {
                NavHost(navController = navController, startDestination = FIRST) {
                    composable(FIRST) { Text("First", modifier = Modifier.testTag(FIRST)) }
                    composable(SECOND) { Text("Second", modifier = Modifier.testTag(SECOND)) }
                }
            }
        }
        composeRule.runOnIdle { navController.navigate(SECOND) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SECOND).assertExists()
    }

    private companion object {
        val SHOWN = NetworkDialogState.Shown(checking = false, stillOffline = false)
        const val CONTENT = "content"
        const val FIRST = "first"
        const val SECOND = "second"
    }
}
