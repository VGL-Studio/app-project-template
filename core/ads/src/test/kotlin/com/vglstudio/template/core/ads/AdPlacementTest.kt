package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Placements of the staging ad gallery: one per format, staging only, same rule as scripts/release.sh (plan T-014). */
class AdPlacementTest {
    private val galleryPlacements = AdPlacement.entries.filter { it.key.startsWith("gallery_") }

    @Test
    fun galleryPlacements_coverEveryFormatOnce() {
        assertEquals(7, galleryPlacements.size)
        assertEquals(
            setOf(
                AdFormat.NATIVE_SMALL,
                AdFormat.NATIVE_MEDIUM,
                AdFormat.NATIVE_COLLAPSIBLE,
                AdFormat.INTERSTITIAL,
                AdFormat.NATIVE_FULL_SCREEN,
            ),
            galleryPlacements.map { it.format }.toSet(),
        )
        val keys = AdPlacement.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun stagingOnly_isExactlyGalleryPrefix() {
        // Control: both kinds exist, so the check below compares true and false values.
        assertTrue(AdPlacement.entries.any { it.key.startsWith("gallery_") })
        assertTrue(AdPlacement.entries.any { !it.key.startsWith("gallery_") })
        AdPlacement.entries.forEach { placement ->
            // Same prefix as the one scripts/release.sh skips (PR #81), written out on purpose.
            assertEquals(placement.key, placement.key.startsWith("gallery_"), placement.stagingOnly)
        }
    }

    @Test
    fun otherPlacements_areNotStagingOnly() {
        assertFalse(AdPlacement.SAMPLE_NATIVE_SMALL.stagingOnly)
        AdPlacement.entries.filterNot { it.key.startsWith("gallery_") }.forEach { placement ->
            assertFalse(placement.key, placement.stagingOnly)
        }
        // Control: a gallery placement is staging only, so the property does not always return false.
        assertTrue(AdPlacement.GALLERY_NATIVE_SMALL.stagingOnly)
    }
}
