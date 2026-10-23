package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdUnitIdsTest {
    @Test
    fun configuredUnits_areParsedPerPlacementKey() {
        val ids = ConfiguredAdUnitIds(" sample_native_small = unit/1 ; other_key=unit/2;")
        assertEquals("unit/1", ids.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertTrue(ids.missing.isEmpty())
    }

    @Test
    fun placementWithoutUnit_isReportedMissingAndShowsNoAd() {
        val ids = ConfiguredAdUnitIds("")
        assertEquals(AdPlacement.entries.toList(), ids.missing)
        assertNull(ids.of(AdPlacement.SAMPLE_NATIVE_SMALL))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedEntry_isRejected() {
        ConfiguredAdUnitIds("sample_native_small")
    }

    @Test
    fun googleTestUnits_existForEveryPlacement() {
        AdPlacement.entries.forEach { placement ->
            assertTrue(GoogleTestAdUnitIds.of(placement).startsWith("ca-app-pub-3940256099942544/"))
        }
    }

    @Test
    fun googleTestUnits_nativeFormats() {
        assertEquals("ca-app-pub-3940256099942544/2247696110", GoogleTestAdUnitIds.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        // NATIVE_MEDIUM has no placement in the app yet (T-014 adds one): checked through the format table.
        assertEquals("ca-app-pub-3940256099942544/1044960115", GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_MEDIUM))
        assertEquals("ca-app-pub-3940256099942544/2247696110", GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_SMALL))
    }

    @Test
    fun googleTestUnit_existsForEveryFormat() {
        AdFormat.entries.forEach { format ->
            assertTrue(
                "$format has no test unit",
                GoogleTestAdUnitIds.unitFor(format).startsWith("ca-app-pub-3940256099942544/"),
            )
        }
        assertEquals("ca-app-pub-3940256099942544/1044960115", GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_FULL_SCREEN))
    }

    @Test
    fun everyPlacementKey_isUniqueAndUsableInRemoteConfig() {
        val keys = AdPlacement.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        keys.forEach { assertTrue("Key '$it' must be lower_snake_case", Regex("^[a-z0-9_]+$").matches(it)) }
    }

    @Test
    fun nonPersonalizedAds_onlyInRegulatedRegionsWithoutRemoteSwitch() {
        assertTrue(requestsNonPersonalizedAds(inRegulatedRegion = true, personalizedInRegulatedRegions = false))
        assertFalse(requestsNonPersonalizedAds(inRegulatedRegion = true, personalizedInRegulatedRegions = true))
        assertFalse(requestsNonPersonalizedAds(inRegulatedRegion = false, personalizedInRegulatedRegions = false))
    }
}
