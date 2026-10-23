package com.vglstudio.template.core.ads

import com.vglstudio.template.core.config.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The defaults are business decisions of the product owner (ADR-0014): change them only with a new decision. */
class AdsSettingsTest {
    private fun assertKey(key: RemoteKey<*>, name: String, type: RemoteKey.Type, default: Any) {
        assertEquals(name, key.name)
        assertEquals(type, key.type)
        assertEquals(default, key.default)
    }

    @Test
    fun placementsAreOffByDefault() {
        AdPlacement.entries.forEach { placement ->
            assertKey(
                AdsSettings.placementEnabled(placement),
                "ads_${placement.key}_enabled",
                RemoteKey.Type.BOOLEAN,
                false,
            )
        }
    }

    @Test
    fun personalizedAdsInRegulatedRegionsAreOnByDefault() {
        assertKey(
            AdsSettings.PERSONALIZED_IN_REGULATED_REGIONS,
            "ads_personalized_regulated_regions_enabled",
            RemoteKey.Type.BOOLEAN,
            true,
        )
    }

    @Test
    fun interstitialMinIntervalIs45SecondsByDefault() {
        assertKey(
            AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS,
            "ads_interstitial_min_interval_seconds",
            RemoteKey.Type.LONG,
            45L,
        )
    }

    @Test
    fun adsAreEnabledByDefault() {
        assertKey(AdsSettings.ENABLED, "ads_enabled", RemoteKey.Type.BOOLEAN, true)
    }

    @Test
    fun firstSessionGraceIs180Seconds() {
        assertKey(AdsSettings.FIRST_SESSION_GRACE_SECONDS, "ads_first_session_grace_seconds", RemoteKey.Type.LONG, 180L)
    }

    @Test
    fun interstitialMaxPerSessionIs4() {
        assertKey(
            AdsSettings.INTERSTITIAL_MAX_PER_SESSION_COUNT,
            "ads_interstitial_max_per_session_count",
            RemoteKey.Type.LONG,
            4L,
        )
    }

    @Test
    fun inListEveryIs8() {
        assertKey(AdsSettings.INLIST_EVERY_COUNT, "ads_inlist_every_count", RemoteKey.Type.LONG, 8L)
    }

    @Test
    fun nativeLoadTimeoutIs10Seconds() {
        assertKey(AdsSettings.NATIVE_LOAD_TIMEOUT_SECONDS, "ads_native_load_timeout_seconds", RemoteKey.Type.LONG, 10L)
    }

    @Test
    fun allKeys_containsEveryDeclaredKey() {
        val declared = AdsSettings::class.java.declaredFields
            .filter { RemoteKey::class.java.isAssignableFrom(it.type) }
            .map { field ->
                field.isAccessible = true
                field.get(AdsSettings) as RemoteKey<*>
            }
        // Control: the reflection finds the keys declared so far, so an empty list cannot pass by accident.
        assertTrue("Found only ${declared.map { it.name }}", declared.size >= 7)
        val all = AdsSettings.all()
        declared.forEach { key -> assertTrue("$key is missing from AdsSettings.all()", key in all) }
    }

    @Test
    fun allKeys_containsEveryPlacement() {
        val all = AdsSettings.all()
        AdPlacement.entries.forEach { placement ->
            assertTrue("$placement is missing", AdsSettings.placementEnabled(placement) in all)
        }
    }

    @Test
    fun allKeys_haveUniqueNames() {
        val names = AdsSettings.all().map { it.name }
        // Control: the 7 named keys and one key per placement are there, so an empty list cannot pass.
        assertTrue("Only $names", names.size >= 7 + AdPlacement.entries.size)
        assertEquals(names.distinct(), names)
    }
}
