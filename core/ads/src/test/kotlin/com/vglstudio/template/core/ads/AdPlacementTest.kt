package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

    @Test
    fun intervalGroupOnNonInterstitial_isRejected() {
        // Control: the same group on an interstitial is accepted.
        AdPlacementDeclaration("inter_sounds", AdFormat.INTERSTITIAL, AdIntervalGroup.named("sounds", 30))
        listOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_FULL_SCREEN, AdFormat.APP_OPEN, AdFormat.REWARDED).forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, AdIntervalGroup.named("sounds", 30))
            }
        }
    }

    @Test
    fun nativeEveryOnNonListFormat_isRejected() {
        // Control: the list formats accept it.
        listOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_IN_COLLECTION).forEach {
            AdPlacementDeclaration("list_x", it, nativeEvery = 6)
        }
        listOf(AdFormat.BANNER, AdFormat.INTERSTITIAL, AdFormat.NATIVE_COLLAPSIBLE, AdFormat.NATIVE_STEP).forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, nativeEvery = 6)
            }
        }
    }

    @Test
    fun nativeEveryBelowOne_isRejected() {
        // Control: 1 is accepted.
        AdPlacementDeclaration("list_x", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 1)
        listOf(0, -1).forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("list_x", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = it)
            }
        }
    }

    @Test
    fun inListGalleryPlacements_declareNativeEvery8() {
        assertEquals(8, AdPlacement.GALLERY_INLIST_SMALL.nativeEvery)
        assertEquals(8, AdPlacement.GALLERY_INLIST_MEDIUM.nativeEvery)
        val inList = setOf(AdPlacement.GALLERY_INLIST_SMALL, AdPlacement.GALLERY_INLIST_MEDIUM)
        AdPlacement.entries.filterNot { it in inList }
            .forEach { assertNull(it.key, it.nativeEvery) }
    }

    @Test
    fun everyPlacement_isInDefaultGroup() {
        // Control: groups compare by name and default, so the check below is not an identity check.
        assertEquals(AdIntervalGroup.named("sounds", 30), AdIntervalGroup.named("sounds", 30))
        assertFalse(AdIntervalGroup.named("sounds", 30) == AdIntervalGroup.named("sounds", 40))
        assertFalse(AdIntervalGroup.named("sounds", 30) == AdIntervalGroup.DEFAULT)
        AdPlacement.entries.forEach { assertEquals(it.key, AdIntervalGroup.DEFAULT, it.intervalGroup) }
    }

    @Test
    fun intervalAttributes_defaultAndValidated() {
        AdPlacement.entries.forEach {
            assertFalse(it.key, it.ignoreInterval)
            assertTrue(it.key, it.recordInterval)
            assertFalse(it.key, it.declaration.ignoreInterval)
            assertTrue(it.key, it.declaration.recordInterval)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AdPlacementDeclaration("x", AdFormat.NATIVE_SMALL, ignoreInterval = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AdPlacementDeclaration("x", AdFormat.APP_OPEN, recordInterval = false)
        }
        // Control: the full-screen formats accept both attributes.
        AdPlacementDeclaration("x", AdFormat.INTERSTITIAL, recordInterval = false)
        AdPlacementDeclaration("x", AdFormat.NATIVE_FULL_SCREEN, ignoreInterval = true)
    }
}
