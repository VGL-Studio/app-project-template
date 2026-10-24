package com.vglstudio.template

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * The mediation SDKs bring start-up providers, receivers and permissions into the app.
 * core/ads/src/main/AndroidManifest.xml removes the AppLovin provider and two AppLovin permissions and keeps VIBRATE
 * (plan T-015), and removes the Meta provider and the exported Mintegral receiver (plan T-034): no network code runs
 * before consent (ADR-0004, ADR-0026). The permission list is fixed: a new permission from an SDK upgrade fails the
 * test, so a plan decides it.
 * This reads the main merged manifest of stagingDebug: the unit test manifest (`android_merged_manifest`) does not
 * apply `tools:node="remove"`. The working directory of a unit test is the module directory.
 */
class MergedManifestTest {
    private val manifest: Document by lazy {
        val file = File(MERGED_MANIFEST)
        assertTrue("Missing ${file.absolutePath}; run :app:processStagingDebugMainManifest", file.isFile)
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)
    }

    private fun names(tag: String): List<String> {
        val nodes = manifest.getElementsByTagName(tag)
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NS, "name") }
    }

    @Test
    fun mergedManifest_containsAppLovinActivity() {
        assertTrue(
            "No com.applovin activity: the AppLovin SDK is not in the build, or this is the wrong file",
            names("activity").any { it.startsWith("com.applovin.") },
        )
    }

    @Test
    fun mergedManifest_hasNoAppLovinInitProvider() {
        assertFalse(
            "AppLovinInitProvider runs before consent",
            "com.applovin.sdk.AppLovinInitProvider" in names("provider"),
        )
    }

    @Test
    fun mergedManifest_hasNoCheckLicensePermission() {
        assertFalse("com.android.vending.CHECK_LICENSE" in names("uses-permission"))
    }

    @Test
    fun mergedManifest_hasNoWifiStatePermission() {
        assertFalse("android.permission.ACCESS_WIFI_STATE" in names("uses-permission"))
    }

    @Test
    fun mergedManifest_keepsVibratePermission() {
        // The AppLovin SDK vibrates without checking the permission or catching the error.
        assertTrue("android.permission.VIBRATE" in names("uses-permission"))
    }

    @Test
    fun mergedManifest_containsActivityOfEveryNetwork() {
        val activities = names("activity")
        val prefixes = listOf(
            "com.facebook.ads.",
            "com.fyber.inneractive.",
            "com.inmobi.ads.",
            "com.ironsource.",
            "com.mbridge.msdk.",
            "com.moloco.sdk.",
            "com.bytedance.sdk.openadsdk.",
            "com.unity3d.ads.",
        )
        for (prefix in prefixes) {
            assertTrue(
                "No $prefix activity: that network's SDK is not in the build",
                activities.any { it.startsWith(prefix) },
            )
        }
    }

    @Test
    fun mergedManifest_permissionsAreExactlyTheAllowedSet() {
        val expected = listOf(
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "android.permission.ACCESS_ADSERVICES_TOPICS",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.INTERNET",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "com.google.android.gms.permission.AD_ID",
            "com.vglstudio.template.staging.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )
        assertEquals(expected, names("uses-permission").sorted())
    }

    @Test
    fun mergedManifest_hasNoMetaAudienceNetworkProvider() {
        assertFalse(
            "AudienceNetworkContentProvider starts the Meta SDK before consent",
            "com.facebook.ads.AudienceNetworkContentProvider" in names("provider"),
        )
    }

    @Test
    fun mergedManifest_hasNoMintegralNetworkReceiver() {
        assertFalse(
            "NetWorkChangeReceiver is exported and never gets the system broadcast it filters",
            "com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver" in names("receiver"),
        )
    }

    @Test
    fun mergedManifest_hasNoAppLovinSdkKey() {
        // The adapter reads the SDK key from the AdMob mediation mapping only; the app carries no key (plan T-034).
        assertFalse(
            "applovin.sdk.key must not be in the app",
            "applovin.sdk.key" in names("meta-data"),
        )
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val MERGED_MANIFEST =
            "build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml"
    }
}
