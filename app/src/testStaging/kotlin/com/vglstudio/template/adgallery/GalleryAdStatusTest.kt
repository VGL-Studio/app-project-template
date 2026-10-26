package com.vglstudio.template.adgallery

import com.vglstudio.template.core.ads.ConsentPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the "Ad status" page of the gallery shows (KB-11, plan T-036, step 5). */
class GalleryAdStatusTest {
    private val switches = FakeAdsFlags().global.value

    @Test
    fun temporaryRight_onlyCachedEarlyOrErrorAllowed_whileAllowed() {
        val temporary = mutableListOf<Pair<ConsentPath?, Boolean>>()
        val paths: List<ConsentPath?> = ConsentPath.entries + null
        for (path in paths) {
            for (canRequestAds in listOf(true, false)) {
                val status = galleryAdStatus(
                    path = path,
                    canRequestAds = canRequestAds,
                    privacyOptionsRequired = false,
                    sdkReady = true,
                    switches = switches,
                )
                assertEquals(path, status.consentPath)
                assertEquals(canRequestAds, status.adsAllowed)
                if (status.temporaryRight) temporary += path to canRequestAds
            }
        }
        // Exactly the two paths that open the gate for now (ConsentGate.kt), and only while ads are allowed.
        assertEquals(
            setOf(ConsentPath.CACHED_EARLY to true, ConsentPath.ERROR_ALLOWED to true),
            temporary.toSet(),
        )
    }

    @Test
    fun otherFlags_copied() {
        val status = galleryAdStatus(
            path = ConsentPath.GRANTED,
            canRequestAds = true,
            privacyOptionsRequired = true,
            sdkReady = false,
            switches = switches,
        )
        assertTrue(status.privacyOptionsRequired)
        assertFalse(status.sdkReady)
        // Control (L-055): the opposite values come through too.
        val other = galleryAdStatus(ConsentPath.GRANTED, true, privacyOptionsRequired = false, sdkReady = true, switches)
        assertFalse(other.privacyOptionsRequired)
        assertTrue(other.sdkReady)
    }

    @Test
    fun switches_null_arePending() {
        val status = galleryAdStatus(ConsentPath.GRANTED, true, false, true, switches = null)

        assertNull(status.consentShortcutRemote)
        assertNull(status.nativeAfterInter)
        assertNull(status.nativeBottom)
        assertNull(status.nativeCollapsible)
    }

    @Test
    fun switches_copied() {
        val status = galleryAdStatus(
            path = ConsentPath.GRANTED,
            canRequestAds = true,
            privacyOptionsRequired = false,
            sdkReady = true,
            switches = switches.copy(
                consentShortcutEnabled = true,
                showNativeAfterInter = false,
                showNativeBottom = true,
                showNativeCollap = false,
            ),
        )

        assertEquals(true, status.consentShortcutRemote)
        assertEquals(false, status.nativeAfterInter)
        assertEquals(true, status.nativeBottom)
        assertEquals(false, status.nativeCollapsible)

        // Control (L-055): each switch is read from its own field.
        val swapped = galleryAdStatus(
            path = ConsentPath.GRANTED,
            canRequestAds = true,
            privacyOptionsRequired = false,
            sdkReady = true,
            switches = switches.copy(
                consentShortcutEnabled = false,
                showNativeAfterInter = true,
                showNativeBottom = false,
                showNativeCollap = true,
            ),
        )
        assertEquals(false, swapped.consentShortcutRemote)
        assertEquals(true, swapped.nativeAfterInter)
        assertEquals(false, swapped.nativeBottom)
        assertEquals(true, swapped.nativeCollapsible)
    }

    @Test
    fun unknown_isPendingEverywhere() {
        val unknown = GalleryAdStatus.Unknown
        assertNull(unknown.consentPath)
        assertFalse(unknown.adsAllowed)
        assertFalse(unknown.temporaryRight)
        assertNull(unknown.consentShortcutRemote)
        assertNull(unknown.nativeAfterInter)
    }
}
