package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.LogLevel
import com.vglstudio.template.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AppLovin MAX starts once, after the consent gate and after AdMob is ready, only with an SDK key and an order that
 * has MAX (plan T-068, KB-31, ADR-0037, L-072). [MaxSdkStarter] is the only caller of [MaxPlatform.initialize].
 */
class MaxSdkStarterTest {
    private val logger = RecordingAppLogger()

    private fun starter(config: MaxConfig = MaxConfig(FAKE_KEY), platform: MaxPlatform) =
        MaxSdkStarter(config, platform, logger)

    private fun messages() = logger.entries.map { it.message }

    @Test
    fun notStarted_beforeAdmobReady() {
        val platform = FakeMaxPlatform()
        val starter = starter(platform = platform)
        // Remote Config may answer before consent: the second way to initialize must wait too (L-072).
        repeat(3) { starter.onOrderChanged(MediationOrder.MAX_ADMOB) }
        starter.onOrderChanged(MediationOrder.MAX)
        assertEquals(0, platform.calls)
        // Control (L-055): the same starter starts once AdMob is ready.
        starter.onAdmobReady(MediationOrder.MAX_ADMOB)
        assertEquals(1, platform.calls)
    }

    @Test
    fun started_onceWhenAdmobReady() {
        val platform = FakeMaxPlatform()
        val starter = starter(platform = platform)
        starter.onAdmobReady(MediationOrder.MAX_ADMOB)
        starter.onAdmobReady(MediationOrder.MAX_ADMOB)
        starter.onOrderChanged(MediationOrder.MAX)
        assertEquals(1, platform.calls)
        assertEquals(listOf(FAKE_KEY), platform.initializedKeys)
        assertFalse(starter.ready.value)
        platform.finishInitialization()
        assertTrue(starter.ready.value)
        assertTrue(messages().toString(), "MAX started" in messages())
        assertTrue(messages().toString(), "MAX ready" in messages())
    }

    @Test
    fun noKey_neverStarts() {
        listOf(MaxConfig.NONE, MaxConfig("   ")).forEach { config ->
            val platform = FakeMaxPlatform()
            val log = RecordingAppLogger()
            val starter = MaxSdkStarter(config, platform, log)
            starter.onAdmobReady(MediationOrder.MAX_ADMOB)
            starter.onOrderChanged(MediationOrder.MAX)
            starter.onOrderChanged(MediationOrder.ADMOB_MAX)
            assertEquals(0, platform.calls)
            val lines = log.entries.map { it.message }
            assertEquals(lines.toString(), 1, lines.count { it == "MAX skipped: no SDK key" })
            assertFalse(lines.toString(), lines.any { config.sdkKey.isNotBlank() && config.sdkKey in it })
        }
    }

    @Test
    fun admobOrder_waitsForOrderWithMax() {
        val platform = FakeMaxPlatform()
        val starter = starter(platform = platform)
        starter.onAdmobReady(MediationOrder.ADMOB)
        assertEquals(0, platform.calls)
        starter.onOrderChanged(MediationOrder.ADMOB_MAX)
        assertEquals(1, platform.calls)
        starter.onOrderChanged(MediationOrder.ADMOB)
        starter.onOrderChanged(MediationOrder.MAX)
        assertEquals(1, platform.calls)
    }

    @Test
    fun alreadyInitializedByAdmob_warnsAndStarts() {
        val platform = FakeMaxPlatform(isInitialized = true)
        starter(platform = platform).onAdmobReady(MediationOrder.MAX_ADMOB)
        val warnings = logger.entries.filter { "already started by AdMob mediation" in it.message }
        assertEquals(logger.entries.toString(), 1, warnings.size)
        assertEquals(LogLevel.WARN, warnings.single().level)
        assertTrue(warnings.single().message, "ADR-0037" in warnings.single().message)
        assertEquals(1, platform.calls)

        // Control: an SDK not yet started gives no warning.
        val quiet = RecordingAppLogger()
        val fresh = FakeMaxPlatform(isInitialized = false)
        MaxSdkStarter(MaxConfig(FAKE_KEY), fresh, quiet).onAdmobReady(MediationOrder.MAX_ADMOB)
        assertEquals(1, fresh.calls)
        assertFalse(quiet.entries.toString(), quiet.entries.any { "already started" in it.message })
    }

    @Test
    fun keyNeverLogged() {
        val platform = FakeMaxPlatform(isInitialized = true)
        val starter = starter(platform = platform)
        starter.onOrderChanged(MediationOrder.MAX_ADMOB)
        starter.onAdmobReady(MediationOrder.ADMOB)
        starter.onOrderChanged(MediationOrder.MAX)
        platform.finishInitialization()
        // Control: something was logged, so the check below reads real lines.
        assertTrue(logger.entries.isNotEmpty())
        assertFalse(logger.entries.toString(), logger.entries.any { FAKE_KEY in it.message })
        assertFalse(MaxConfig(FAKE_KEY).toString(), FAKE_KEY in MaxConfig(FAKE_KEY).toString())
    }

    private companion object {
        /** A fake key of the real length (86 characters), with FAKE so the secret scan lets it through. */
        val FAKE_KEY = "FAKE" + "k".repeat(82)
    }
}
