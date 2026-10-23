package com.vglstudio.template

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * The AppLovin SDK brings a start-up provider and permissions into the app; core/ads/src/main/AndroidManifest.xml
 * removes the provider (no AppLovin code before consent, ADR-0004) and two permissions, and keeps VIBRATE (plan
 * T-015). This reads the main merged manifest of stagingDebug: the unit test manifest (`android_merged_manifest`)
 * does not apply `tools:node="remove"`. The working directory of a unit test is the module directory.
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

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val MERGED_MANIFEST =
            "build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml"
    }
}
