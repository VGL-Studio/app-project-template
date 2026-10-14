package com.vglstudio.template.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFeatureFlagsTest {
    private val flags = DefaultFeatureFlags()

    @Test
    fun everyFlag_returnsItsDefaultValue() {
        FeatureFlag.entries.forEach { flag ->
            assertEquals(flag.defaultValue, flags.isEnabled(flag))
        }
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
