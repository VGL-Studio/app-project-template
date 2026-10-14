package com.vglstudio.template.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A flag is on when the current build enables it locally, otherwise it takes its default value (off).
 * Replaced by a Firebase Remote Config implementation when Firebase is wired in.
 */
class DefaultFeatureFlags @Inject constructor(
    @param:LocallyEnabledFlags private val locallyEnabled: Set<@JvmSuppressWildcards FeatureFlag>,
) : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> = flowOf(flag in locallyEnabled || flag.defaultValue)
}
