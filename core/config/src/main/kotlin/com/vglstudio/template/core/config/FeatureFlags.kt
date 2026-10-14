package com.vglstudio.template.core.config

/** Read access to feature flags. Feature modules use this interface, never Remote Config directly. */
interface FeatureFlags {
    fun isEnabled(flag: FeatureFlag): Boolean
}
