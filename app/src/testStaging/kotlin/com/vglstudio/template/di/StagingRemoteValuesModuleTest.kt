package com.vglstudio.template.di

import com.vglstudio.template.config.AppRemoteKeys
import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.AdsSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Local values of staging builds for the ad keys of plan T-019 (section 3.1, question 3). */
class StagingRemoteValuesModuleTest {
    private val values = StagingRemoteValuesModule.everyAdPlacementEnabled() +
        StagingRemoteValuesModule.globalAdSwitchesOn()
    private val names = values.map { it.key.name }
    private val shownNames = values.filter { it.value == true }.map { it.key.name }.toSet()

    @Test
    fun everyAdPlacement_isShown() {
        // App-wide placements have no is_show_<code>: show_open_ads* switch them (plan T-024, question 3).
        val switched = AdPlacement.entries.filterNot { it.appWide }
        // Control: some placements are app-wide, and the others are still checked.
        assertTrue(AdPlacement.entries.any { it.appWide })
        assertTrue(switched.isNotEmpty())
        switched.forEach { placement ->
            val name = AdsSettings.placementEnabled(placement).name
            // Control: the switch is the key of plan T-019, not the wave 2 one.
            assertEquals("is_show_${placement.key}", name)
            assertTrue("$name is not on", name in shownNames)
        }
    }

    @Test
    fun appWidePlacements_haveNoLocalValue() {
        // Control: placement switches are set, so an is_show_ value would be found if one were there.
        assertTrue(names.toString(), "is_show_sample_native_small" in names)
        assertFalse(names.toString(), names.any { it.startsWith("is_show_open_app") })
    }

    @Test
    fun globalShowKeys_areOn() {
        listOf(
            "show_open_ads",
            "show_open_ads_first_open",
            "show_resum_ads",
            "is_show_native_after_inter",
            "is_show_native_bottom",
            "is_log_ad_fullscreen_events",
            "is_log_ad_slot_events",
        ).forEach { assertTrue("$it is not on", it in shownNames) }
    }

    @Test
    fun consentShortcutAndMasterSwitch_areNotSet() {
        // Control: the global switches are set, so the set looked at below is the one of plan T-019.
        assertTrue(names.toString(), "is_show_native_bottom" in names)
        assertFalse("is_enable_consent_shortcut" in names)
        assertFalse("is_show_ads" in names)
    }

    @Test
    fun everyLocalValue_isADeclaredKey() {
        val declared = AppRemoteKeys.byModule.values.flatten().map { it.name }.toSet()
        // Control: a global switch is set, so the values below are not only placement switches.
        assertTrue(names.toString(), "show_open_ads" in names)
        names.forEach { assertTrue("$it is not a declared key", it in declared) }
        assertEquals(names.size, names.toSet().size)
    }
}
