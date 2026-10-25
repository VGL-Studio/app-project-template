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
    /**
     * Every placement shows Google test ads on staging (ADR-0013); placements added later are included. App-wide
     * placements have no switch of their own: [globalAdSwitchesOn] turns them on (plan T-024).
     */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun everyAdPlacementEnabled(): Set<LocalRemoteValue> = AdPlacement.entries
        .filterNot { it.appWide }
        .map { LocalRemoteValue.of(AdsSettings.placementEnabled(it), true) }
        .toSet()

    /**
     * App open, resume, combo, native bottom slot and the ad lifecycle logs are on for testers (plan T-019,
     * question 3). Left as on production: the master switch (on by default), the consent shortcut (off: it also
     * needs a build setting), the ad types and the interval.
     */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun globalAdSwitchesOn(): Set<LocalRemoteValue> = listOf(
        AdsSettings.SHOW_OPEN_ADS,
        AdsSettings.SHOW_OPEN_ADS_FIRST_OPEN,
        AdsSettings.SHOW_RESUM_ADS,
        AdsSettings.IS_SHOW_NATIVE_AFTER_INTER,
        AdsSettings.IS_SHOW_NATIVE_BOTTOM,
        AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS,
        AdsSettings.IS_LOG_AD_SLOT_EVENTS,
    ).map { LocalRemoteValue.of(it, true) }.toSet()
}
