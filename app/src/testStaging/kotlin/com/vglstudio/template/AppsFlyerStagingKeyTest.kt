package com.vglstudio.template

import org.junit.Assert.assertEquals
import org.junit.Test

/** The staging build has no AppsFlyer dev key, so AppsFlyer never starts and sends nothing (ADR-0027, ADR-0012). */
class AppsFlyerStagingKeyTest {
    @Test
    fun stagingBuild_hasNoDevKey() {
        assertEquals("", BuildConfig.APPSFLYER_DEV_KEY)
    }
}
