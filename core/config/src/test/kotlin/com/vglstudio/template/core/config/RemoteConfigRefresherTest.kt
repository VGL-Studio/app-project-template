package com.vglstudio.template.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigRefresherTest {
    private var fetchCount = 0

    private fun policy(isFake: Boolean = false, fetch: suspend () -> Unit = {}) = RemoteConfigRefreshPolicy(
        isFakeConfig = { isFake },
        fetchAndActivate = {
            fetchCount++
            fetch()
        },
    )

    @Test
    fun fakeConfig_returnsTrueWithoutFetching() = runTest {
        assertTrue(policy(isFake = true).refresh())
        assertEquals(0, fetchCount)
    }

    @Test
    fun fetchSucceeds_returnsTrue() = runTest {
        assertTrue(policy().refresh())
        assertEquals(1, fetchCount)
    }

    @Test
    fun fetchFails_returnsFalse() = runTest {
        assertFalse(policy(fetch = { error("no network") }).refresh())
    }

    @Test
    fun fetchThrottled_returnsTrue() = runTest {
        assertTrue(policy(fetch = { throw FirebaseRemoteConfigFetchThrottledException(0L) }).refresh())
    }

    @Test
    fun fetchLongerThanTenSeconds_returnsFalse() = runTest {
        assertFalse(policy(fetch = { delay(10_001) }).refresh())
    }

    @Test
    fun fetchJustUnderTenSeconds_returnsTrue() = runTest {
        assertTrue(policy(fetch = { delay(9_999) }).refresh())
    }
}
