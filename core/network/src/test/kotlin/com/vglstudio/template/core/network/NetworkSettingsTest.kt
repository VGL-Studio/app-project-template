package com.vglstudio.template.core.network

import com.vglstudio.template.core.config.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The key and its default are decisions of spec network section 8: change them only with a new decision. */
class NetworkSettingsTest {
    @Test
    fun lostDelay_isLong2Seconds() {
        val key = NetworkSettings.LOST_DELAY_SECONDS
        assertEquals("network_lost_delay_seconds", key.name)
        assertEquals(RemoteKey.Type.LONG, key.type)
        assertEquals(2L, key.default)
        assertTrue(key.description.isNotBlank())
    }

    @Test
    fun all_containsEveryDeclaredKey() {
        assertEquals(listOf(NetworkSettings.LOST_DELAY_SECONDS), NetworkSettings.all())
    }
}
