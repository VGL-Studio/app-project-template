package com.vglstudio.template.core.ads

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether a full-screen ad is on screen; the network popup waits for it to close (ADR-0024, plan T-017). Inside
 * [FullScreenAdPresence.publishAfter] the flag is published once, at the end (plan T-054).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FullScreenAdPresenceTest {
    private val published = mutableListOf<Boolean>()

    @Test
    fun startsHidden() {
        // Control: a presence that was told an ad is showing reads true, so the flow is live.
        val shown = FullScreenAdPresence().apply { onShowing() }
        assertTrue(shown.showing.value)

        assertFalse(FullScreenAdPresence().showing.value)
    }

    @Test
    fun onShowing_setsTrue() {
        val presence = FullScreenAdPresence()
        presence.onShowing()
        assertTrue(presence.showing.value)
    }

    @Test
    fun onClosed_setsFalse() {
        val presence = FullScreenAdPresence()
        presence.onShowing()
        assertTrue(presence.showing.value)
        presence.onClosed()
        assertFalse(presence.showing.value)
    }

    @Test
    fun closedTwice_staysFalse() {
        val presence = FullScreenAdPresence()
        presence.onShowing()
        // Control: the ad is reported as showing before it is closed twice.
        assertTrue(presence.showing.value)
        presence.onClosed()
        presence.onClosed()
        assertFalse(presence.showing.value)
    }

    @Test
    fun showingFlow_emitsShowAndClose() = runTest {
        val presence = FullScreenAdPresence()
        presence.showing.test {
            assertEquals(false, awaitItem())
            presence.onShowing()
            assertEquals(true, awaitItem())
            presence.onClosed()
            assertEquals(false, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun secondShowWhileShowing_isDropped() {
        val presence = FullScreenAdPresence()
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 0))
        assertFalse(presence.tryBegin(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, hostActivityId = 1, now = 1_000))
        presence.onClosed()
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, hostActivityId = 1, now = 2_000))
    }

    @Test
    fun showingFlag_expiresAfterTenMinutes() {
        val presence = FullScreenAdPresence()
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 0))
        assertFalse(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 599_999))
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 2, now = 600_000))
        assertEquals(FullScreenAdPresence.Shown(AdPlacement.GALLERY_INTERSTITIAL, 2), presence.current)
    }

    @Test
    fun current_followsTheAdOnScreen() {
        val presence = FullScreenAdPresence()
        presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 7, now = 0)
        assertEquals(FullScreenAdPresence.Shown(AdPlacement.GALLERY_INTERSTITIAL, 7), presence.current)
        assertTrue(presence.showing.value)
        presence.onClosed()
        assertEquals(null, presence.current)
        assertFalse(presence.showing.value)
    }

    @Test
    fun publishAfter_closeThenBegin_publishesNoFalse() = runTest {
        val presence = FullScreenAdPresence(onPublished = { published += it })
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 0))
        published.clear()
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { presence.showing.collect { values += it } }

        presence.publishAfter {
            presence.onClosed()
            assertTrue(presence.tryBegin(AdPlacement.NATIVE_AFTER_INTER, hostActivityId = 0, now = 1_000))
        }
        job.cancel()

        assertEquals(listOf(true), values)
        assertEquals(emptyList<Boolean>(), published)
        assertEquals(FullScreenAdPresence.Shown(AdPlacement.NATIVE_AFTER_INTER, 0), presence.current)
    }

    @Test
    fun publishAfter_closeOnly_publishesFalseOnceAtEnd() = runTest {
        val presence = FullScreenAdPresence(onPublished = { published += it })
        assertTrue(presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, hostActivityId = 1, now = 0))
        published.clear()
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { presence.showing.collect { values += it } }

        presence.publishAfter {
            presence.onClosed()
            // The inner state moves at once (F-01), the published flag waits for the end of the block.
            assertFalse(presence.isBusy(1_000))
            assertEquals(null, presence.current)
            assertTrue(presence.showing.value)
            assertEquals(emptyList<Boolean>(), published)
        }
        job.cancel()

        assertFalse(presence.showing.value)
        assertEquals(listOf(true, false), values)
        assertEquals(listOf(false), published)
    }

    @Test
    fun publishAfter_nested_outerPublishes() {
        val presence = FullScreenAdPresence(onPublished = { published += it })
        presence.onShowing()
        assertEquals(listOf(true), published)

        presence.publishAfter {
            presence.publishAfter { presence.onClosed() }
            // The inner block does not publish.
            assertTrue(presence.showing.value)
            assertEquals(listOf(true), published)
        }
        assertFalse(presence.showing.value)
        assertEquals(listOf(true, false), published)

        // Control (L-055): out of a block, a close publishes at once.
        presence.onShowing()
        presence.onClosed()
        assertFalse(presence.showing.value)
        assertEquals(listOf(true, false, true, false), published)
    }

    @Test
    fun publishAfter_blockThrows_stillPublishesFinalValue() {
        val presence = FullScreenAdPresence(onPublished = { published += it })
        presence.onShowing()
        published.clear()

        assertThrows(IllegalStateException::class.java) {
            presence.publishAfter {
                presence.onClosed()
                // Control (L-055): inside the block the flag is not published yet.
                assertTrue(presence.showing.value)
                throw IllegalStateException("onFinished failed")
            }
        }

        assertFalse(presence.showing.value)
        assertEquals(listOf(false), published)
    }

    @Test
    fun showingNow_readsInnerStateInsideBlock() {
        val presence = FullScreenAdPresence()
        presence.onShowing()
        assertTrue(presence.showingNow)

        presence.publishAfter {
            presence.onClosed()
            assertFalse(presence.showingNow)
            assertTrue(presence.showing.value)
        }

        assertFalse(presence.showingNow)
        assertFalse(presence.showing.value)
    }
}
