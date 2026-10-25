package com.vglstudio.template.core.ads

import app.cash.turbine.test
import com.vglstudio.template.core.config.RemoteKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The placement shown when the app opens (A-01, A-02) and the switch of every placement (L-072), plan T-024. */
@OptIn(ExperimentalCoroutinesApi::class)
class StartupAdChoiceTest {
    private val settings = FakeRemoteSettings()
    private val flags = DefaultAdsFlags(settings)

    private fun FakeRemoteSettings.set(name: String, value: Boolean) =
        set(RemoteKey.boolean(name, false, "Test key."), value)

    private fun FakeRemoteSettings.set(name: String, value: String) =
        set(RemoteKey.string(name, "", "Test key."), value)

    private suspend fun globalFlags(): AdsGlobalFlags = flags.global.first()

    /** A first-open mark that is never read. */
    private val neverRead: Flow<Boolean> = flow { awaitCancellation() }

    @Test
    fun firstOpen_readsFirstOpenKeyOnly() = runTest {
        val global = globalFlags().copy(showOpenAdsFirstOpen = true, showOpenAds = false)
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(global, firstOpen = true))
        // Control (L-055): the same keys on a later open show nothing.
        assertNull(startupAdPlacement(global, firstOpen = false))
    }

    @Test
    fun laterOpen_readsShowOpenAdsOnly() = runTest {
        val global = globalFlags().copy(showOpenAds = true, showOpenAdsFirstOpen = false)
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(global, firstOpen = false))
        assertNull(startupAdPlacement(global, firstOpen = true))
    }

    @Test
    fun masterSwitchOff_isNull() = runTest {
        settings.set("show_open_ads", true)
        settings.set("show_open_ads_first_open", true)
        // Control: with the master switch on, both opens show an ad.
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(globalFlags(), firstOpen = true))
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(globalFlags(), firstOpen = false))
        settings.set("is_show_ads", false)
        assertNull(startupAdPlacement(globalFlags(), firstOpen = true))
        assertNull(startupAdPlacement(globalFlags(), firstOpen = false))
    }

    @Test
    fun interType_choosesOpenAppInter() = runTest {
        val on = globalFlags().copy(showOpenAds = true, openAdType = OpenAdType.INTERSTITIAL)
        assertEquals(AdPlacement.OPEN_APP_INTER, startupAdPlacement(on, firstOpen = false))
        assertEquals(
            AdPlacement.OPEN_APP,
            startupAdPlacement(on.copy(openAdType = OpenAdType.APP_OPEN), firstOpen = false),
        )
        assertNull(startupAdPlacement(on.copy(showOpenAds = false), firstOpen = false))
    }

    @Test
    fun placementSwitch_followsKeysAndFirstOpen() = runTest {
        settings.set("show_open_ads_first_open", true)
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(true)).first())
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP_INTER, flags, flowOf(true)).first())
        settings.set("open_ad_type", "inter")
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(true)).first())
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP_INTER, flags, flowOf(true)).first())
    }

    @Test
    fun placementSwitch_keyTurnedOff_emitsFalse() = runTest {
        settings.set("show_open_ads_first_open", true)
        placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(true)).test {
            assertEquals(true, awaitItem())
            settings.set("show_open_ads_first_open", false)
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun placementSwitch_waitsForFirstOpen() = runTest {
        settings.set("show_open_ads_first_open", true)
        settings.set("show_open_ads", true)
        placementSwitch(AdPlacement.OPEN_APP, flags, neverRead).test {
            advanceTimeBy(1_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        // Control: once the mark is read, the switch has a value.
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
    }

    @Test
    fun placementSwitch_otherPlacements_unchanged() = runTest {
        val placement = AdPlacement.SAMPLE_NATIVE_SMALL
        // Other placements do not wait for the first-open mark, and follow is_show_<code> as before.
        assertEquals(false, placementSwitch(placement, flags, neverRead).first())
        settings.set("is_show_sample_native_small", true)
        assertEquals(true, placementSwitch(placement, flags, neverRead).first())
        settings.set("is_show_ads", false)
        assertEquals(false, placementSwitch(placement, flags, emptyFlow()).first())
        settings.set("is_show_ads", true)
        // Control (L-055): an app-wide placement does not follow a switch of its own name.
        settings.set("is_show_open_app", true)
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
    }
}
