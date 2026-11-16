package com.vglstudio.template.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.analytics.AdMetricEvent
import com.vglstudio.template.core.analytics.RecordingAdMetrics
import com.vglstudio.template.core.logging.RecordingAppLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [rememberRewardedAdRequired]: whether a tap on a rewarded action would open its dialog now (R-08). It reads the
 * very gate [RewardedAction.start] reads, so a mark on the screen and the dialog never disagree.
 */
@RunWith(AndroidJUnit4::class)
class RewardedAdRequiredTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION), true)
        // On, a dialog or an opportunity is recorded if there is one.
        set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
    }
    private val adFree = MutableStateFlow(false)
    private val metrics = RecordingAdMetrics()
    private var required: Boolean? = null
    private var observed = mutableListOf<Boolean?>()
    private var action: RewardedAction? = null
    private var granted = 0

    private fun controller(): AdsController {
        val status = object : AdFreeStatus {
            override val adFree = this@RewardedAdRequiredTest.adFree
        }
        return testAdsController(
            unitIds = AdUnitIds { null },
            flags = DefaultAdsFlags(settings, status),
            metrics = metrics,
            logger = RecordingAppLogger(),
        ).also { controller ->
            composeRule.runOnUiThread {
                controller.start(composeRule.activity.application, AppActivityFilter { true })
            }
            composeRule.waitForIdle()
        }
    }

    private fun setRequired(controller: AdsController, withAction: Boolean = false) {
        composeRule.setContent {
            AdsHost(controller) {
                AdsTestTheme {
                    required = rememberRewardedAdRequired(AdPlacement.GALLERY_RW_ACTION)
                    if (withAction) {
                        val current = rememberRewardedAction(AdPlacement.GALLERY_RW_ACTION, onGranted = { granted++ })
                        action = current
                        RewardedAdDialog(current, title = "Title")
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun change(block: () -> Unit): Boolean? {
        composeRule.runOnIdle(block)
        composeRule.waitForIdle()
        return required
    }

    private fun dialogs(): Int = composeRule.onAllNodesWithTag(RewardedAdDialogTestTags.CARD).fetchSemanticsNodes().size

    @Test
    fun noController_false() {
        composeRule.setContent {
            AdsTestTheme { required = rememberRewardedAdRequired(AdPlacement.GALLERY_RW_ACTION) }
        }
        composeRule.waitForIdle()

        assertEquals(false, required)
    }

    @Test
    fun beforeFirstValue_null_thenFollowsTheGate() {
        val gate = MutableSharedFlow<Boolean>(replay = 1)
        composeRule.setContent {
            val value = rememberRewardedAdRequiredOf(gate)
            observed += value
        }
        composeRule.waitForIdle()
        // No value yet: never guessed (as start() waits for the first value).
        assertEquals(listOf<Boolean?>(null), observed.distinct())

        composeRule.runOnIdle { gate.tryEmit(true) }
        composeRule.waitForIdle()
        assertEquals(true, observed.last())

        composeRule.runOnIdle { gate.tryEmit(false) }
        composeRule.waitForIdle()
        assertEquals(false, observed.last())
    }

    @Test
    fun realController_followsEveryPartOfTheGate() {
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRequired(controller)
        // Control: every part open, the dialog would open.
        assertEquals(true, required)

        val key = AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION)
        assertEquals("is_show_<code> off", false, change { settings.set(key, false) })
        assertEquals("is_show_<code> back on", true, change { settings.set(key, true) })
        assertEquals("is_show_ads off", false, change { settings.set(AdsSettings.IS_SHOW_ADS, false) })
        assertEquals("is_show_ads back on", true, change { settings.set(AdsSettings.IS_SHOW_ADS, true) })
        assertEquals("ad-free", false, change { adFree.value = true })
        assertEquals("ad-free ended", true, change { adFree.value = false })
        assertEquals("consent gate closed", false, change { controller.closeConsentGateForTest() })
        assertEquals("consent gate open again", true, change { controller.reopenConsentGateForTest() })
    }

    @Test
    fun realController_sameAnswerAsStart_gateOpen_dialogOpens() {
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRequired(controller, withAction = true)
        assertEquals(true, required)

        composeRule.runOnIdle { action!!.start() }
        composeRule.waitForIdle()

        assertEquals(1, dialogs())
        assertEquals(0, granted)
    }

    @Test
    fun realController_sameAnswerAsStart_gateClosed_actionRunsAtOnce() {
        val controller = controller()
        // Consent gate never opened: the gate counts as off.
        setRequired(controller, withAction = true)
        assertEquals(false, required)

        composeRule.runOnIdle { action!!.start() }
        composeRule.waitForIdle()

        assertEquals(0, dialogs())
        assertEquals(1, granted)
    }

    @Test
    fun realController_readingRecordsNothing() {
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRequired(controller)
        assertEquals(true, required)
        change { adFree.value = true }
        change { adFree.value = false }

        assertTrue(
            metrics.events.toString(),
            metrics.events.none { it is AdMetricEvent.RewardDialog || it is AdMetricEvent.Opportunity },
        )
    }

    @Test
    fun nonRewardedPlacement_isRejected() {
        val error = runCatching {
            composeRule.setContent { rememberRewardedAdRequired(AdPlacement.SAMPLE_NATIVE_SMALL) }
            composeRule.waitForIdle()
        }.exceptionOrNull()

        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }
}
