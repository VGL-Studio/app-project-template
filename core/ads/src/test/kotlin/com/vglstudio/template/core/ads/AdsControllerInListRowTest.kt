package com.vglstudio.template.core.ads

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The native small row of a list in the real [AdsController] (plan T-059): each send of the row waits for the
 * in-screen window of T-054 (foreground, no full-screen ad over the app, KB-27), and a failed row tries again 5 s
 * later under its own log key. A fake native loader ([AdsController.nativeLoaderForTest]) takes the requests.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerInListRowTest {
    private val logger = RecordingAppLogger()
    private val loader = FakeNativeAdLoader()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val controller = testAdsController(
        unitIds = InScreenOnlyUnitIds,
        flags = DefaultAdsFlags(FakeRemoteSettings()),
        logger = logger,
    ).also { it.nativeLoaderForTest = loader }

    @After
    fun tearDown() {
        scope.cancel()
        idle()
    }

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun openGate() {
        controller.openConsentGateForTest()
        controller.markSdkReadyForTest()
    }

    /** A standalone native full-screen ad over the app. */
    private fun cover() {
        assertTrue(
            controller.beginStandaloneNativeTurn(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, { FakeLoadedNativeAd() }) {},
        )
        assertFalse(controller.inScreenAdWindow.value)
    }

    /** Closes the standalone turn of [cover], once its countdown is over (K-03). */
    private fun uncover() {
        if (controller.nativeFullScreenView.value?.closeEnabled != true) idleFor(NATIVE_AFTER_INTER_COUNTDOWN_MILLIS)
        controller.closeNativeFullScreen()
        assertTrue(controller.inScreenAdWindow.value)
    }

    private fun collect(states: Flow<NativeAdState>): MutableList<NativeAdState> {
        val received = mutableListOf<NativeAdState>()
        scope.launch { states.collect { received += it } }
        idle()
        return received
    }

    private fun row(slot: Int) =
        controller.inListRowStates(AdPlacement.GALLERY_INLIST_SMALL, slot, MutableStateFlow(true))

    @Test
    fun inListRow_waitsWhileCovered_thenSends() {
        openGate()
        controller.setAppForegroundForTest(true)
        idle()
        // Control (L-055): not covered, a row sends at once.
        val free = collect(row(1))
        assertEquals(1, loader.requests.size)
        loader.deliver(FakeLoadedNativeAd())
        idle()
        assertTrue(free.last() is NativeAdState.Loaded)

        cover()
        collect(row(0))
        idleFor(60_000)
        assertEquals(1, loader.requests.size)

        uncover()
        idle()
        assertEquals(2, loader.requests.size)
    }

    @Test
    fun inListRow_inBackground_sendsNothing() {
        openGate()
        controller.setAppForegroundForTest(false)
        idle()

        collect(row(0))
        idleFor(60_000)
        assertEquals(0, loader.requests.size)

        controller.setAppForegroundForTest(true)
        idle()
        assertEquals(1, loader.requests.size)
    }

    @Test
    fun inListRow_failedThenRetriesAt5s_rowLogKey() {
        openGate()
        controller.setAppForegroundForTest(true)
        idle()

        val states = collect(row(0))
        assertEquals(1, loader.requests.size)
        loader.fail(3)
        idle()
        assertEquals(NativeAdState.Unavailable, states.last())

        idleFor(4_999)
        assertEquals(1, loader.requests.size)
        idleFor(1)
        assertEquals(2, loader.requests.size)
        assertTrue(
            logger.entries.map { it.message }.toString(),
            logger.entries.any { it.message == "Native gallery_inlist_small#0 retry in 5000 ms (failures 1)" },
        )
    }

    /** Test units for the in-screen native formats only: no full-screen ad or banner of this test sends anything. */
    private object InScreenOnlyUnitIds : AdUnitIds {
        override fun of(placement: AdPlacement): String? = of(placement, placement.format)

        override fun of(placement: AdPlacement, format: AdFormat): String? = when (format) {
            AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_COLLAPSIBLE ->
                GoogleTestAdUnitIds.unitFor(format)
            else -> null
        }
    }
}
