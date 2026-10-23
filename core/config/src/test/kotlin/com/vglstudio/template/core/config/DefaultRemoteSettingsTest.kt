package com.vglstudio.template.core.config

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRemoteSettingsTest {
    private val remote = FakeRemoteFlagSource()
    private val settings = DefaultRemoteSettings(remote)

    private val someSwitch = RemoteKey.boolean("some_switch", true, "A switch.")
    private val someInterval = RemoteKey.long("some_interval", 120L, "An interval.")
    private val someRatio = RemoteKey.double("some_ratio", 0.5, "A ratio.")
    private val otherRatio = RemoteKey.double("other_ratio", 0.5, "Another ratio.")
    private val someLanguages = RemoteKey.string("some_languages", "vi,en", "Languages.")

    @Test
    fun booleanAndLong_withoutRemoteValue_useDefault() = runTest {
        // Control: values of other keys sit next to the keys read, and must not leak into them.
        remote.booleans.value = mapOf("other_switch" to false)
        remote.longs.value = mapOf("other_interval" to 30L)
        assertTrue(settings.value(someSwitch).first())
        assertEquals(120L, settings.value(someInterval).first())
    }

    @Test
    fun booleanAndLong_remoteValue_replacesDefault() = runTest {
        remote.booleans.value = mapOf("some_switch" to false)
        remote.longs.value = mapOf("some_interval" to 30L)
        assertFalse(settings.value(someSwitch).first())
        assertEquals(30L, settings.value(someInterval).first())
    }

    @Test
    fun remoteValueActivatedLater_reachesCollectors() = runTest {
        settings.value(someInterval).test {
            assertEquals(120L, awaitItem())
            remote.longs.value = mapOf("some_interval" to 45L)
            assertEquals(45L, awaitItem())
        }
    }

    @Test
    fun doubleKey_withoutRemoteValue_usesDefault() = runTest {
        // Control: another double key has a remote value, which this key must not take.
        remote.doubles.value = mapOf("other_ratio" to 0.25)
        assertEquals(0.5, settings.value(someRatio).first(), 0.0)
        assertEquals(0.25, settings.value(otherRatio).first(), 0.0)
    }

    @Test
    fun doubleKey_remoteValue_replacesDefault() = runTest {
        remote.doubles.value = mapOf("some_ratio" to 0.25)
        assertEquals(0.25, settings.value(someRatio).first(), 0.0)
    }

    @Test
    fun stringKey_withoutRemoteValue_usesDefault() = runTest {
        // Control: another string key has a remote value, which this key must not take.
        remote.strings.value = mapOf("other_languages" to "hi")
        assertEquals("vi,en", settings.value(someLanguages).first())
    }

    @Test
    fun stringKey_remoteValue_replacesDefault() = runTest {
        remote.strings.value = mapOf("some_languages" to "hi")
        assertEquals("hi", settings.value(someLanguages).first())
    }

    @Test
    fun stringKey_remoteEmptyString_isKept() = runTest {
        remote.strings.value = mapOf("some_languages" to "")
        assertEquals("", settings.value(someLanguages).first())
    }

    @Test
    fun valueOfAnotherType_usesDefault() = runTest {
        remote.longs.value = mapOf("some_ratio" to 3L)
        // Control: a double value of another key is read, so doubles do reach the settings.
        remote.doubles.value = mapOf("other_ratio" to 0.25)
        assertEquals(0.5, settings.value(someRatio).first(), 0.0)
        assertEquals(0.25, settings.value(otherRatio).first(), 0.0)
    }
}
