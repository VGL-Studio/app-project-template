package com.vglstudio.template.core.ads

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AdsRemoteRulesTest {
    private val settings = FakeRemoteSettings()
    private val placement = AdPlacement.entries.first()

    @Test
    fun masterSwitchOff_turnsEveryPlacementOff() = runTest {
        settings.set(AdsSettings.placementEnabled(placement), true)
        placementActive(settings, placement).test {
            assertEquals(true, awaitItem())
            settings.set(AdsSettings.ENABLED, false)
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun masterSwitchBackOn_restoresEnabledPlacements() = runTest {
        settings.set(AdsSettings.placementEnabled(placement), true)
        settings.set(AdsSettings.ENABLED, false)
        placementActive(settings, placement).test {
            assertEquals(false, awaitItem())
            settings.set(AdsSettings.ENABLED, true)
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // A placement that is switched off stays off when the master switch comes back on.
        val placementOff = FakeRemoteSettings().apply { set(AdsSettings.ENABLED, false) }
        placementActive(placementOff, placement).test {
            assertEquals(false, awaitItem())
            placementOff.set(AdsSettings.ENABLED, true)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(false, placementActive(placementOff, placement).first())
    }

    @Test
    fun placementOffByDefault_evenWithMasterSwitchOn() = runTest {
        // Control: the same placement switched on is active.
        val other = FakeRemoteSettings().apply { set(AdsSettings.placementEnabled(placement), true) }
        assertEquals(true, placementActive(other, placement).first())
        settings.set(AdsSettings.ENABLED, true)
        assertEquals(false, placementActive(settings, placement).first())
    }

    @Test
    fun limitsUseDefaults() = runTest {
        assertEquals(
            FullScreenAdLimits(
                adsEnabled = true,
                minIntervalMillis = 45_000,
                firstSessionGraceMillis = 180_000,
                maxPerSession = 4,
            ),
            fullScreenAdLimits(settings).first(),
        )
    }

    @Test
    fun limitsFollowRemoteValues() = runTest {
        settings.set(AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS, 60L)
        settings.set(AdsSettings.FIRST_SESSION_GRACE_SECONDS, 0L)
        settings.set(AdsSettings.INTERSTITIAL_MAX_PER_SESSION_COUNT, 2L)
        assertEquals(
            FullScreenAdLimits(
                adsEnabled = true,
                minIntervalMillis = 60_000,
                firstSessionGraceMillis = 0,
                maxPerSession = 2,
            ),
            fullScreenAdLimits(settings).first(),
        )
    }

    @Test
    fun negativeRemoteValues_areClampedToZero() = runTest {
        // Control: before the negative values arrive, the limits are not zero.
        assertEquals(45_000L, fullScreenAdLimits(settings).first().minIntervalMillis)
        settings.set(AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS, -5L)
        settings.set(AdsSettings.FIRST_SESSION_GRACE_SECONDS, -1L)
        settings.set(AdsSettings.INTERSTITIAL_MAX_PER_SESSION_COUNT, -3L)
        assertEquals(
            FullScreenAdLimits(
                adsEnabled = true,
                minIntervalMillis = 0,
                firstSessionGraceMillis = 0,
                maxPerSession = 0,
            ),
            fullScreenAdLimits(settings).first(),
        )
    }
}
