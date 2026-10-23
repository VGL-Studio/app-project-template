package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class AdFormatTest {
    @Test
    fun fullScreenFormats_areInterstitialAndNativeFullScreen() {
        val fullScreen = AdFormat.entries.filter { it.isFullScreen }
        assertEquals(listOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN), fullScreen)
        AdFormat.entries.filterNot { it in fullScreen }.forEach { format ->
            assertEquals("$format must not be full screen", false, format.isFullScreen)
        }
    }
}
