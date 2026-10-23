package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Preloaded native full-screen ads are shown only within an hour of loading (plan T-013, question 4). */
class FreshAdCacheTest {
    private val discarded = mutableListOf<String>()
    private val cache = FreshAdCache<String>(onDiscard = { discarded += it })
    private val key = AdPlacement.SAMPLE_NATIVE_SMALL

    @Test
    fun emptyCache_hasNothingFresh() {
        assertFalse(cache.hasFresh(key, nowMillis = 0))
        assertNull(cache.take(key, nowMillis = 0))
        assertFalse(cache.contains(key))

        // Control: once an ad is put, the same reads see it.
        cache.put(key, "ad", nowMillis = 0)
        assertTrue(cache.contains(key))
        assertTrue(cache.hasFresh(key, nowMillis = 0))
        assertEquals("ad", cache.take(key, nowMillis = 0))
    }

    @Test
    fun adWithinOneHour_isTaken() {
        cache.put(key, "ad", nowMillis = 0)
        assertTrue(cache.hasFresh(key, nowMillis = 3_599_999))
        assertEquals("ad", cache.take(key, nowMillis = 3_599_999))
        assertNull(cache.take(key, nowMillis = 3_599_999))
        assertTrue(discarded.isEmpty())
    }

    @Test
    fun adOlderThanOneHour_isDiscarded() {
        cache.put(key, "ad", nowMillis = 0)
        assertFalse(cache.hasFresh(key, nowMillis = 3_600_000))
        assertNull(cache.take(key, nowMillis = 3_600_000))
        assertEquals(listOf("ad"), discarded)
        assertFalse(cache.contains(key))
    }

    @Test
    fun put_replacesAndDiscardsOlderAd() {
        cache.put(key, "first", nowMillis = 0)
        cache.put(key, "second", nowMillis = 10)
        assertEquals(listOf("first"), discarded)
        assertEquals("second", cache.take(key, nowMillis = 20))
    }
}
