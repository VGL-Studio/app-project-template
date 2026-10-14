package com.vglstudio.template.core.config

import javax.inject.Inject

/**
 * Returns the default value of every flag.
 * Replaced by a Firebase Remote Config implementation when Firebase is wired in.
 */
class DefaultFeatureFlags @Inject constructor() : FeatureFlags {
    override fun isEnabled(flag: FeatureFlag): Boolean = flag.defaultValue
}
