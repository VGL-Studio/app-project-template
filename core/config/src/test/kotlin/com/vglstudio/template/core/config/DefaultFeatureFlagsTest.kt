package com.vglstudio.template.core.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFeatureFlagsTest {
    @Test
    fun everyFlag_isOffByDefault() {
        // Rule (ADR-0003): a feature stays hidden in production until it is approved for release.
        FeatureFlag.entries.forEach { flag ->
            assertFalse("Flag ${flag.name} must default to off", flag.defaultValue)
        }
    }

    @Test
    fun withoutLocalOverrides_everyFlagIsOff() {
        val flags = DefaultFeatureFlags(locallyEnabled = emptySet())
        FeatureFlag.entries.forEach { flag -> assertFalse(flags.isEnabled(flag)) }
    }

    @Test
    fun locallyEnabledFlag_isOn() {
        val flags = DefaultFeatureFlags(locallyEnabled = setOf(FeatureFlag.SAMPLE))
        assertTrue(flags.isEnabled(FeatureFlag.SAMPLE))
    }

    @Test
    fun everyFlagKey_followsNamingRule() {
        FeatureFlag.entries.forEach { flag ->
            assertTrue(
                "Flag key '${flag.key}' must match feature_<name>_enabled",
                Regex("^feature_[a-z0-9_]+_enabled$").matches(flag.key),
            )
        }
    }
}
