package com.vglstudio.template.core.ads

import com.google.android.gms.ads.mediation.Adapter
import org.junit.Assert.assertTrue
import org.junit.Test

/** The AppLovin mediation adapter ships in :core:ads, so AdMob can serve AppLovin ads without code (KB-9). */
class MediationAdapterClasspathTest {
    @Test
    fun appLovinAdapter_isOnClasspath() {
        // initialize = false: the static initializer is not run, so no Android API is called on the JVM.
        val adapter = Class.forName(
            "com.google.ads.mediation.applovin.AppLovinMediationAdapter",
            false,
            javaClass.classLoader,
        )
        assertTrue(
            "${adapter.name} is not a Google Mobile Ads mediation adapter",
            Adapter::class.java.isAssignableFrom(adapter),
        )
    }
}
