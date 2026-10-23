package com.vglstudio.template.di

import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.AdsSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StagingRemoteValuesModuleTest {
    private val values = StagingRemoteValuesModule.everyAdPlacementEnabled()

    @Test
    fun everyAdPlacement_isEnabled() {
        val enabledKeyNames = values.filter { it.value == true }.map { it.key.name }.toSet()
        val placementKeyNames = AdPlacement.entries.map { AdsSettings.placementEnabled(it).name }.toSet()
        assertEquals(placementKeyNames, enabledKeyNames)
    }

    @Test
    fun onlyPlacementKeys_areSet() {
        val placementKey = Regex("^ads_[a-z0-9_]+_enabled$")
        val names = values.map { it.key.name }
        // Control: the set is not empty, so the checks below look at real elements.
        assertTrue(names.isNotEmpty())
        names.forEach { assertTrue("$it is not a placement key", placementKey.matches(it)) }
        assertFalse(AdsSettings.ENABLED.name in names)
    }
}
