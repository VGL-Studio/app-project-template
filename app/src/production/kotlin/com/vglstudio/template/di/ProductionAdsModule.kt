package com.vglstudio.template.di

import com.vglstudio.template.BuildConfig
import com.vglstudio.template.core.ads.AdUnitIds
import com.vglstudio.template.core.ads.AdsDebugOptions
import com.vglstudio.template.core.ads.ConfiguredAdUnitIds
import com.vglstudio.template.core.ads.ConsentDebugOptions
import com.vglstudio.template.core.ads.ConsentShortcutConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Production uses the real ad units given when the release is built (ADR-0009, ADR-0013). */
@Module
@InstallIn(SingletonComponent::class)
object ProductionAdsModule {
    @Provides
    fun adUnitIds(): AdUnitIds = ConfiguredAdUnitIds(BuildConfig.AD_UNITS)

    @Provides
    fun consentDebugOptions(): ConsentDebugOptions = ConsentDebugOptions.NONE

    /** The consent shortcut is off in production builds (S-12). */
    @Provides
    fun consentShortcutConfig(): ConsentShortcutConfig = ConsentShortcutConfig.OFF

    /** No test device and no per-network log in production builds (plan T-058, KB-28). */
    @Provides
    fun adsDebugOptions(): AdsDebugOptions = AdsDebugOptions.NONE
}
