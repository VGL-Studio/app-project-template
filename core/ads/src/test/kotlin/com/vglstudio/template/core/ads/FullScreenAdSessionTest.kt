package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class FullScreenAdSessionTest {
    private val session = FullScreenAdSession(timeoutMillis = 1_800_000)

    @Test
    fun newSession_hasNoAdShown() {
        // Control: an earlier session that counted an ad does not leak into a new one.
        val earlier = FullScreenAdSession().apply { onShown() }
        assertEquals(1, earlier.shownCount)
        assertEquals(0, session.shownCount)
    }

    @Test
    fun onShown_incrementsCount() {
        session.onShown()
        session.onShown()
        assertEquals(2, session.shownCount)
    }

    @Test
    fun shortBackground_keepsCount() {
        session.onShown()
        // Control: the ad is counted before the app goes to background.
        assertEquals(1, session.shownCount)
        session.onBackground(now = 0)
        session.onForeground(now = 1_799_999)
        assertEquals(1, session.shownCount)
    }

    @Test
    fun backgroundOfTimeout_startsNewSession() {
        session.onShown()
        // Control: the ad is counted before the app goes to background.
        assertEquals(1, session.shownCount)
        session.onBackground(now = 0)
        session.onForeground(now = 1_800_000)
        assertEquals(0, session.shownCount)
    }

    @Test
    fun foregroundWithoutBackground_keepsCount() {
        session.onShown()
        // Control: the ad is counted before the app comes to foreground.
        assertEquals(1, session.shownCount)
        session.onForeground(now = 10_000_000)
        assertEquals(1, session.shownCount)
    }
}
