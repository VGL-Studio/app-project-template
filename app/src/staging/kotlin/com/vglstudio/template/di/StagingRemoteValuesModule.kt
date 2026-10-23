package com.vglstudio.template.di

import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.AdsSettings
import com.vglstudio.template.core.config.LocalRemoteValue
import com.vglstudio.template.core.config.LocalRemoteValues
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet

/**
 * Remote Config values of staging builds only, used when Firebase has no value for the key (ADR-0023).
 * A value set on the staging Firebase project still wins, so the team can switch a placement off remotely.
 * Production builds have no such module: keys there read Firebase, then their default.
 */
@Module
@InstallIn(SingletonComponent::class)
object StagingRemoteValuesModule {
    /** Every placement shows Google test ads on staging (ADR-0013); placements added later are included. */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun everyAdPlacementEnabled(): Set<LocalRemoteValue> =
        AdPlacement.entries.map { LocalRemoteValue.of(AdsSettings.placementEnabled(it), true) }.toSet()
}
