package com.vglstudio.template.core.ads

import com.vglstudio.template.core.ads.AdPlatform.ADMOB
import com.vglstudio.template.core.ads.AdPlatform.MAX
import com.vglstudio.template.core.ads.FullScreenLoadState.Idle
import com.vglstudio.template.core.ads.FullScreenLoadState.Loading
import com.vglstudio.template.core.ads.FullScreenLoadState.Ready
import com.vglstudio.template.core.ads.FullScreenLoadState.WaitingRetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The platforms of an interstitial turn (plan T-069, KB-31, question 41 of the ads spec). */
class FullScreenPlatformsTest {
    private val inApp = AdPlacement.GALLERY_INTERSTITIAL

    @Test
    fun interstitialPlatforms_followOrder_dropMissingUnits() {
        val both = mapOf(
            MediationOrder.ADMOB to listOf(ADMOB),
            MediationOrder.MAX to listOf(MAX),
            MediationOrder.ADMOB_MAX to listOf(ADMOB, MAX),
            MediationOrder.MAX_ADMOB to listOf(MAX, ADMOB),
        )
        both.forEach { (order, expected) ->
            assertEquals(
                "$order",
                expected,
                interstitialPlatforms(inApp, order, hasAdmobUnit = true, hasMaxUnit = true),
            )
        }
        // A platform without its ad unit is dropped (like bannerPlatforms).
        assertEquals(listOf(ADMOB), interstitialPlatforms(inApp, MediationOrder.MAX_ADMOB, true, hasMaxUnit = false))
        assertEquals(listOf(MAX), interstitialPlatforms(inApp, MediationOrder.MAX_ADMOB, hasAdmobUnit = false, true))
        assertEquals(
            emptyList<AdPlatform>(),
            interstitialPlatforms(inApp, MediationOrder.MAX, true, hasMaxUnit = false),
        )
        assertEquals(emptyList<AdPlatform>(), interstitialPlatforms(inApp, MediationOrder.ADMOB, false, true))
    }

    @Test
    fun interstitialPlatforms_openAppInter_admobOnly_anyOrder() {
        MediationOrder.entries.forEach { order ->
            assertEquals(
                "$order",
                listOf(ADMOB),
                interstitialPlatforms(AdPlacement.OPEN_APP_INTER, order, hasAdmobUnit = true, hasMaxUnit = true),
            )
        }
        assertEquals(
            emptyList<AdPlatform>(),
            interstitialPlatforms(
                AdPlacement.OPEN_APP_INTER,
                MediationOrder.MAX,
                hasAdmobUnit = false,
                hasMaxUnit = true,
            ),
        )
    }

    @Test
    fun interstitialPlatforms_resumeInter_followsOrder() {
        assertEquals(
            listOf(MAX, ADMOB),
            interstitialPlatforms(
                AdPlacement.RESUME_INTER,
                MediationOrder.MAX_ADMOB,
                hasAdmobUnit = true,
                hasMaxUnit = true,
            ),
        )
        assertEquals(
            listOf(MAX),
            interstitialPlatforms(AdPlacement.RESUME_INTER, MediationOrder.MAX, hasAdmobUnit = true, hasMaxUnit = true),
        )
    }

    @Test
    fun readyPlatform_firstWithAd_elseNext_elseNull() {
        assertEquals(MAX, readyPlatform(listOf(MAX, ADMOB), admobReady = true, maxReady = true))
        assertEquals(ADMOB, readyPlatform(listOf(MAX, ADMOB), admobReady = true, maxReady = false))
        assertEquals(MAX, readyPlatform(listOf(ADMOB, MAX), admobReady = false, maxReady = true))
        assertEquals(ADMOB, readyPlatform(listOf(ADMOB, MAX), admobReady = true, maxReady = true))
        assertNull(readyPlatform(listOf(ADMOB, MAX), admobReady = false, maxReady = false))
        // A platform not in the list is never picked, even with an ad.
        assertNull(readyPlatform(listOf(ADMOB), admobReady = false, maxReady = true))
        assertNull(readyPlatform(emptyList(), admobReady = true, maxReady = true))
    }

    @Test
    fun turnLoadState_readyWhenAny_elseFirstPlatformState_idleWhenNone() {
        val retry = WaitingRetry(5_000)
        assertEquals(Ready, turnLoadState(listOf(MAX, ADMOB), admob = Ready, max = retry))
        assertEquals(Ready, turnLoadState(listOf(MAX, ADMOB), admob = Idle, max = Ready))
        assertEquals(retry, turnLoadState(listOf(MAX, ADMOB), admob = Loading, max = retry))
        assertEquals(Loading, turnLoadState(listOf(ADMOB, MAX), admob = Loading, max = retry))
        // Only the platforms of the turn count.
        assertEquals(Idle, turnLoadState(listOf(ADMOB), admob = Idle, max = Ready))
        assertEquals(Idle, turnLoadState(emptyList(), admob = Ready, max = Ready))
    }

    @Test
    fun preloadSettleState_loadingWhileAnyLoads() {
        assertEquals(Loading, preloadSettleState(listOf(MAX, ADMOB), admob = Loading, max = Ready))
        assertEquals(Loading, preloadSettleState(listOf(MAX, ADMOB), admob = Ready, max = Loading))
        assertEquals(Ready, preloadSettleState(listOf(MAX, ADMOB), admob = Ready, max = Idle))
        // A platform not in the turn does not hold the group.
        assertEquals(Ready, preloadSettleState(listOf(ADMOB), admob = Ready, max = Loading))
        assertEquals(Idle, preloadSettleState(emptyList(), admob = Loading, max = Loading))
    }
}
