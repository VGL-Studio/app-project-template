package com.vglstudio.template.core.ads

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.analytics.RecordingAdMetrics
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.designsystem.theme.Spacing
import com.vglstudio.template.core.logging.RecordingAppLogger
import com.vglstudio.template.core.network.InternetConnectivity
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast

/**
 * The entry of a feature into a rewarded ad and its dialog, on Robolectric (KB-19, R-01, MC-05; plan T-031). The
 * controller is replaced by fakes through [rememberRewardedActionWith], except in the tests named `realController_*`,
 * which go through [rememberRewardedAction] and a real [AdsController] with no SDK.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RewardedActionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val enabled = MutableSharedFlow<Boolean>(replay = 1)
    private var granted = 0
    private var startCalls = 0
    private val handlers = mutableListOf<(RewardedOutcome) -> Unit>()
    private val events = mutableListOf<String>()
    private val notified = mutableListOf<String>()
    private var action: RewardedAction? = null
    private var dialogBack: OnBackPressedDispatcher? = null
    private var scheme: ColorScheme? = null

    private val watchLabel get() = composeRule.activity.getString(R.string.ads_reward_watch)
    private val noThanksLabel get() = composeRule.activity.getString(R.string.ads_reward_no_thanks)

    @Composable
    private fun Subject(dark: Boolean = false, lines: Int = 1, flow: Flow<Boolean> = enabled) {
        AppTheme(darkTheme = dark) {
            scheme = MaterialTheme.colorScheme
            val current = rememberRewardedActionWith(
                placement = AdPlacement.GALLERY_RW_ACTION,
                enabled = flow,
                start = { onOutcome ->
                    startCalls++
                    handlers += onOutcome
                },
                onDialogEvent = { events += it },
                onGranted = { granted++ },
                notify = { notified += it },
            )
            action = current
            RewardedAdDialog(current, title = TITLE) {
                dialogBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                Column {
                    repeat(lines) { Text("Line ${it + 1}", Modifier.testTag("line${it + 1}")) }
                }
            }
        }
    }

    private fun start() {
        composeRule.runOnIdle { action!!.start() }
        composeRule.waitForIdle()
    }

    private fun tapWatch() {
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.WATCH).performClick()
        composeRule.waitForIdle()
    }

    private fun emit(outcome: RewardedOutcome) {
        composeRule.runOnIdle { handlers.last()(outcome) }
        composeRule.waitForIdle()
    }

    private fun back() {
        composeRule.runOnIdle { dialogBack!!.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun cards(): Int =
        composeRule.onAllNodesWithTag(RewardedAdDialogTestTags.CARD).fetchSemanticsNodes().size

    @Test
    fun switchOn_start_showsDialogWithBothButtonsAndAppContent() {
        composeRule.setContent { Subject() }
        // Control: no dialog before start().
        assertEquals(0, cards())

        enabled.tryEmit(true)
        start()

        assertEquals(1, cards())
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.WATCH).assert(hasText(watchLabel))
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.NO_THANKS).assert(hasText(noThanksLabel))
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.CONTENT).assertExists()
        composeRule.onNodeWithTag("line1", useUnmergedTree = true).assertExists()
        assertEquals(0, granted)
    }

    @Test
    fun noThanks_closesDialog_runsNothing_logsShowThenDismiss() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()

        composeRule.onNodeWithTag(RewardedAdDialogTestTags.NO_THANKS).performClick()
        composeRule.waitForIdle()

        assertEquals(0, cards())
        assertEquals(0, granted)
        assertEquals(0, startCalls)
        assertEquals(listOf("show", "dismiss"), events)
    }

    @Test
    fun backPress_dismissesLikeNoThanks() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()

        back()

        assertEquals(0, cards())
        assertEquals(0, granted)
        assertEquals(0, startCalls)
        assertEquals(listOf("show", "dismiss"), events)
        assertTrue(!composeRule.activity.isFinishing)
    }

    @Test
    fun watchButton_is56dpHigh_fullCardWidth() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()

        val watch = composeRule.onNodeWithTag(RewardedAdDialogTestTags.WATCH).getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(RewardedAdDialogTestTags.CARD).getUnclippedBoundsInRoot()
        assertEquals(56f, watch.height.value, 0.5f)
        assertEquals((card.width - Spacing.lg * 2).value, watch.width.value, 0.5f)
    }

    @Test
    fun noThanksText_usesOnSurfaceVariant_light_and_dark() {
        var dark by mutableStateOf(false)
        composeRule.setContent { Subject(dark = dark) }
        enabled.tryEmit(true)
        start()

        listOf(false, true).forEach { mode ->
            dark = mode
            composeRule.waitForIdle()
            val colors = checkNotNull(scheme)
            val bitmap = dialogBitmap()
            val card = windowBounds(RewardedAdDialogTestTags.CARD)
            val noThanks = windowBounds(RewardedAdDialogTestTags.NO_THANKS)
            // Control: the card itself, in its left padding, away from the text and the round corners.
            val background = bitmap.getPixel((card.left + px(8.dp)).toInt(), card.center.y.toInt())
            assertColor("dark=$mode card", colors.surfaceContainerHigh, background)
            val text = farthestPixel(bitmap, noThanks, background)
            assertColor("dark=$mode No, thanks text", colors.onSurfaceVariant, text)
        }
    }

    @Test
    fun startTwice_whileDialogOpen_oneDialog_logsShowOnce() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)

        composeRule.runOnIdle {
            action!!.start()
            action!!.start()
        }
        composeRule.waitForIdle()
        start()

        assertEquals(1, cards())
        assertEquals(listOf("show"), events)
    }

    @Test
    fun recreateWhileDialogOpen_keepsDialog_logsShowOnce() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()

        recreate()

        assertEquals(1, cards())
        assertEquals(listOf("show"), events)
        assertEquals(0, granted)
    }

    @Test
    fun recreateAfterWatchTapped_resultOnce() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()
        tapWatch()

        recreate()
        emit(RewardedOutcome.Granted)

        assertEquals(1, granted)
        assertEquals(0, cards())
        assertEquals(1, startCalls)
    }

    @Test
    fun backAfterWatch_closesDialog_resultStillOnce() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()
        tapWatch()

        back()
        assertEquals(0, cards())
        assertEquals(listOf("show"), events)

        emit(RewardedOutcome.Granted)
        assertEquals(1, granted)
    }

    @Test
    fun backgroundWhileDialogOpen_sameDialogOnReturn() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        assertEquals(1, cards())
        assertEquals(0, startCalls)
        assertEquals(listOf("show"), events)
    }

    @Test
    fun switchOff_start_runsActionAtOnce_noDialog() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(false)

        start()
        assertEquals(1, granted)
        assertEquals(0, cards())
        assertTrue(events.isEmpty())

        // Control: switched on, the same start opens the dialog and runs nothing.
        enabled.tryEmit(true)
        start()
        assertEquals(1, cards())
        assertEquals(1, granted)
    }

    @Test
    fun tapBeforeSwitchKnown_waitsForFirstValue() {
        composeRule.setContent { Subject() }

        start()
        assertEquals(0, granted)
        assertEquals(0, cards())

        enabled.tryEmit(true)
        composeRule.waitForIdle()
        assertEquals(1, cards())
        assertEquals(0, granted)
    }

    @Test
    fun tapBeforeSwitchKnown_thenOff_runsActionOnce() {
        // Control of the test above: the first value is off.
        composeRule.setContent { Subject() }

        start()
        start()
        assertEquals(0, granted)

        enabled.tryEmit(false)
        composeRule.waitForIdle()
        assertEquals(1, granted)
        assertEquals(0, cards())
    }

    @Test
    fun noController_start_runsActionAtOnce() {
        composeRule.setContent {
            AppTheme {
                val current = rememberRewardedAction(AdPlacement.GALLERY_RW_ACTION, onGranted = { granted++ })
                action = current
                RewardedAdDialog(current, title = TITLE)
            }
        }

        composeRule.runOnIdle {
            action!!.start()
            assertEquals(1, granted)
        }
        composeRule.waitForIdle()
        assertEquals(1, granted)
        assertEquals(0, cards())
    }

    @Test
    fun nonRewardedPlacement_isRejected() {
        val error = runCatching {
            composeRule.setContent { rememberRewardedAction(AdPlacement.SAMPLE_NATIVE_SMALL, onGranted = {}) }
            composeRule.waitForIdle()
        }.exceptionOrNull()

        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun watch_dialogStaysUntilOnScreen_thenGrantedRunsActionOnce() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()

        tapWatch()
        assertEquals(1, startCalls)
        assertEquals(1, cards())

        emit(RewardedOutcome.OnScreen)
        assertEquals(0, cards())
        assertEquals(0, granted)

        emit(RewardedOutcome.Granted)
        assertEquals(1, granted)
        assertEquals(listOf("show"), events)
    }

    @Test
    fun compositionDisposedBeforeReward_actionStillRuns() {
        var show by mutableStateOf(true)
        composeRule.setContent { if (show) Subject() }
        enabled.tryEmit(true)
        start()
        tapWatch()
        emit(RewardedOutcome.OnScreen)

        show = false
        composeRule.waitForIdle()
        emit(RewardedOutcome.Granted)

        assertEquals(1, granted)
    }

    @Test
    fun compositionKept_rewardRunsActionOnce() {
        // Control of the test above.
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()
        tapWatch()
        emit(RewardedOutcome.OnScreen)
        emit(RewardedOutcome.Granted)

        assertEquals(1, granted)
    }

    @Test
    fun noAdNetwork_closesDialog_notifiesCheckConnection() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()
        tapWatch()

        emit(RewardedOutcome.NoAd(network = true, granted = false))

        assertEquals(0, cards())
        assertEquals(listOf(composeRule.activity.getString(R.string.ads_reward_check_connection)), notified)
        assertEquals(0, granted)
    }

    @Test
    fun noAdNotNetwork_closesDialog_noNotice() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()
        tapWatch()

        emit(RewardedOutcome.NoAd(network = false, granted = false))
        assertEquals(0, cards())
        assertTrue(notified.isEmpty())
        assertEquals(0, granted)

        start()
        tapWatch()
        emit(RewardedOutcome.NoAd(network = false, granted = true))
        assertEquals(0, cards())
        assertEquals(1, granted)
        assertTrue(notified.isEmpty())
    }

    @Test
    fun noAdNetwork_butGranted_noNotice() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()
        tapWatch()

        emit(RewardedOutcome.NoAd(network = true, granted = true))

        assertEquals(1, granted)
        assertTrue(notified.isEmpty())
        assertEquals(0, cards())
    }

    @Test
    fun showRewardedNotice_showsToast() {
        composeRule.runOnUiThread { showRewardedNotice(composeRule.activity, "x") }

        assertEquals("x", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun secondWatchTap_callsStartAgain() {
        composeRule.setContent { Subject() }
        enabled.tryEmit(true)
        start()

        tapWatch()
        tapWatch()

        assertEquals(2, startCalls)
        assertEquals(1, cards())
    }

    @Test
    fun twoActionsSamePlacement_bothDialogsClose() {
        val first = mutableListOf<(RewardedOutcome) -> Unit>()
        val second = mutableListOf<(RewardedOutcome) -> Unit>()
        val actions = mutableListOf<RewardedAction>()
        composeRule.setContent {
            AppTheme {
                listOf(first, second).forEach { calls ->
                    val current = rememberRewardedActionWith(
                        placement = AdPlacement.GALLERY_RW_ACTION,
                        enabled = enabled,
                        start = { calls += it },
                        onDialogEvent = {},
                        onGranted = { granted++ },
                        notify = {},
                    )
                    if (current !in actions) actions += current
                    RewardedAdDialog(current, title = TITLE)
                }
            }
        }
        enabled.tryEmit(true)
        composeRule.runOnIdle { actions.forEach { it.start() } }
        composeRule.waitForIdle()
        assertEquals(2, cards())

        composeRule.runOnIdle { actions.forEach { it.watch() } }
        composeRule.waitForIdle()
        assertEquals(1, first.size)
        assertEquals(1, second.size)
        composeRule.runOnIdle {
            first.single()(RewardedOutcome.OnScreen)
            second.single()(RewardedOutcome.OnScreen)
        }
        composeRule.waitForIdle()

        assertEquals(0, cards())
        assertEquals(0, granted)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun longContent_small_buttonsInsideWindow() {
        composeRule.setContent { Subject(lines = 20) }
        enabled.tryEmit(true)
        start()

        val screenHeight = px(640.dp)
        listOf(RewardedAdDialogTestTags.WATCH, RewardedAdDialogTestTags.NO_THANKS).forEach { tag ->
            val node = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
            val top = node.positionOnScreen.y
            assertTrue("$tag top $top", top >= 0f)
            assertTrue("$tag bottom ${top + node.size.height} > $screenHeight", top + node.size.height <= screenHeight)
        }
        // Control: 20 lines do not fit, so the content scrolls to its last line.
        val content = composeRule.onNodeWithTag(RewardedAdDialogTestTags.CONTENT).fetchSemanticsNode()
        val last = composeRule.onNodeWithTag("line20", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(last.boundsInWindow.bottom > content.boundsInWindow.bottom)

        composeRule.onNodeWithTag(RewardedAdDialogTestTags.CONTENT).performScrollToNode(hasTestTag("line20"))
        composeRule.waitForIdle()
        val scrolledContent = composeRule.onNodeWithTag(RewardedAdDialogTestTags.CONTENT).fetchSemanticsNode()
        val scrolledLast = composeRule.onNodeWithTag("line20", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(scrolledLast.boundsInWindow.bottom <= scrolledContent.boundsInWindow.bottom + 1f)
    }

    @Test
    fun realController_consentClosed_keyOn_runsActionAtOnce_noDialog() {
        // Question 8 of the plan, option (a): a closed consent gate counts as switched off.
        val controller = realController()
        setRealContent(controller)

        start()

        assertEquals(1, granted)
        assertEquals(0, cards())
    }

    @Test
    fun realController_consentOpen_keyOn_watchWithoutAd_closesDialogAndBlocks() {
        val controller = realController()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRealContent(controller)

        start()
        assertEquals(1, cards())
        assertEquals(0, granted)

        tapWatch()

        // No ad loaded (no SDK): the default policy blocks, the dialog closes, nothing runs.
        assertEquals(0, cards())
        assertEquals(0, granted)
        assertTrue(
            logger.entries.any { it.tag == "Ads" && it.message.startsWith("Rewarded gallery_rw_action skipped: not_ready") },
        )
    }

    private val logger = RecordingAppLogger()

    private fun realController(): AdsController {
        val settings = FakeRemoteSettings().apply {
            set(AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION), true)
        }
        return AdsController(
            context = ApplicationProvider.getApplicationContext(),
            unitIds = AdUnitIds { null },
            consentDebug = ConsentDebugOptions.NONE,
            flags = DefaultAdsFlags(settings),
            connectivity = object : InternetConnectivity {
                override val connected = flowOf(true)
            },
            shortcutConfig = ConsentShortcutConfig.OFF,
            metrics = RecordingAdMetrics(),
            logger = logger,
            firstOpenMark = FirstOpenMark(EmptyStore(), RecordingAppLogger()),
            interDisplayCounter = InterDisplayCounter(EmptyStore(), RecordingAppLogger()),
        )
    }

    private fun setRealContent(controller: AdsController) {
        composeRule.setContent {
            AdsHost(controller) {
                AppTheme {
                    val current = rememberRewardedAction(AdPlacement.GALLERY_RW_ACTION, onGranted = { granted++ })
                    action = current
                    RewardedAdDialog(current, title = TITLE)
                }
            }
        }
    }

    /** Recreates the activity and sets the same content again, as an activity does in `onCreate`. */
    private fun recreate() {
        composeRule.activityRule.scenario.recreate()
        composeRule.activityRule.scenario.onActivity { it.setContent { Subject() } }
        composeRule.waitForIdle()
    }

    private fun dialogBitmap(): Bitmap {
        val view = ShadowDialog.getLatestDialog().window!!.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun windowBounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow

    private fun px(dp: Dp): Float = with(composeRule.density) { dp.toPx() }

    /** The pixel of [area] most unlike [background]: a pixel inside a letter stroke. */
    private fun farthestPixel(bitmap: Bitmap, area: Rect, background: Int): Int {
        var best = background
        var bestDistance = -1
        for (y in area.top.toInt() until area.bottom.toInt()) {
            for (x in area.left.toInt() until area.right.toInt()) {
                val pixel = bitmap.getPixel(x, y)
                val distance = channels(pixel).zip(channels(background)).sumOf { (a, b) -> abs(a - b) }
                if (distance > bestDistance) {
                    bestDistance = distance
                    best = pixel
                }
            }
        }
        return best
    }

    private fun assertColor(what: String, expected: Color, actual: Int) {
        val want = channels(expected.toArgb())
        val got = channels(actual)
        assertTrue(
            "$what: expected ${Integer.toHexString(expected.toArgb())}, was ${Integer.toHexString(actual)}",
            want.zip(got).all { (a, b) -> abs(a - b) <= COLOR_TOLERANCE },
        )
    }

    private fun channels(argb: Int) = listOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)

    /** A preferences store that is always empty and drops every write. */
    private class EmptyStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(emptyPreferences())
    }

    private companion object {
        const val TITLE = "Unlock export"
        const val COLOR_TOLERANCE = 8
    }
}
