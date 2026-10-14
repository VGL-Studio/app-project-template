package com.vglstudio.template.core.config

import javax.inject.Inject

/**
 * A flag is on when the current build enables it locally, otherwise it takes its default value (off).
 * Replaced by a Firebase Remote Config implementation when Firebase is wired in.
 */
class DefaultFeatureFlags @Inject constructor(
    @param:LocallyEnabledFlags private val locallyEnabled: Set<@JvmSuppressWildcards FeatureFlag>,
) : FeatureFlags {
    override fun isEnabled(flag: FeatureFlag): Boolean = flag in locallyEnabled || flag.defaultValue
}
