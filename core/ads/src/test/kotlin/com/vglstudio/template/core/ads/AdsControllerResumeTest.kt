package com.vglstudio.template.core.ads

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.analytics.AdMetricEvent
import com.vglstudio.template.core.analytics.RecordingAdMetrics
import com.vglstudio.template.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The real [AdsController] tied to the resume ad (plan T-025, L-072): an ad click raises the resume block, the gate
 * reads the controller's own state, and a switched-off resume records nothing. No SDK ad is needed.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerResumeTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings()
    private val metrics = RecordingAdMetrics()
    private val logger = RecordingAppLogger()

    private fun controller() = testAdsController(
        context = application,
        unitIds = AdUnitIds { null },
        flags = DefaultAdsFlags(settings),
        metrics = metrics,
        logger = logger,
    )

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun startedController(): AdsController = controller().also {
        it.start(application, AppActivityFilter { true })
        idle()
    }

    @Test
    fun adClick_raisesResumeSuppression() {
        val controller = startedController()
        assertFalse(controller.resumeAdsSuppressed.value)
        // Any format, even with the lifecycle metrics off and nothing served.
        controller.telemetry.clicked(AdFormat.BANNER, null)
        assertTrue(controller.resumeAdsSuppressed.value)
        // Control: the next resume of an app activity releases it.
        controller.resumeSuppression.onActivityResumed()
        assertFalse(controller.resumeAdsSuppressed.value)
    }

    @Test
    fun resumeOff_gateHasNoPlacement_foregroundRecordsNothing() {
        val controller = startedController()
        assertNull(controller.readResumeGate().placement)
        controller.onProcessForegroundChanged(true)
        controller.onProcessForegroundChanged(false)
        controller.onProcessForegroundChanged(true)
        idle()
        assertTrue(metrics.events.none { it is AdMetricEvent.Opportunity })
        val resumeLogs = logger.entries.filter { it.message.startsWith("Resume ad ") }
        assertEquals(resumeLogs.toString(), 2, resumeLogs.size)
        assertTrue(resumeLogs.toString(), resumeLogs.none { it.message.startsWith("Resume ad SHOW") })
        // Control (L-055): the keys switch the placement of the gate.
        settings.set(AdsSettings.SHOW_RESUM_ADS, true)
        idle()
        assertEquals(AdPlacement.RESUME, controller.readResumeGate().placement)
        settings.set(AdsSettings.RESUME_AD_TYPE, "inter")
        idle()
        assertEquals(AdPlacement.RESUME_INTER, controller.readResumeGate().placement)
    }

    @Test
    fun readResumeGate_mapsControllerState() {
        val controller = controller()
        // Before the keys are read.
        val before = controller.readResumeGate()
        assertNull(before.placement)
        assertFalse(before.serving)
        assertTrue(before.appOpenLockPassed)
        assertFalse(before.ready)
        assertFalse(before.busy)
        assertFalse(before.startupFinished)
        assertFalse(before.suppressed)
        controller.resumeSuppression.markAdClicked()
        assertTrue(controller.readResumeGate().suppressed)

        // The startup key is off: awaitStartupAd returns null and the startup turn is over.
        controller.start(application, AppActivityFilter { true })
        idle()
        assertFalse(controller.readResumeGate().startupFinished)
        var returned = false
        var turn: StartupAdTurn? = null
        CoroutineScope(Dispatchers.Main).launch {
            turn = controller.awaitStartupAd()
            returned = true
        }
        idle()
        assertTrue(returned)
        assertNull(turn)
        assertTrue(controller.readResumeGate().startupFinished)

        // Control: with no loading screen, the turn is over 45 s after start, not before.
        val other = startedController()
        idleFor(44_000)
        assertFalse(other.readResumeGate().startupFinished)
        idleFor(1_100)
        assertTrue(other.readResumeGate().startupFinished)
    }
}
