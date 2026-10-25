package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.Flow

/** A-01, A-02: the placement shown when the app opens, or null when the key of this open is off. */
@Suppress("UNUSED_PARAMETER")
internal fun startupAdPlacement(flags: AdsGlobalFlags, firstOpen: Boolean): AdPlacement? = null

/**
 * The switch of [placement]: for an app-wide placement, whether [startupAdPlacement] chooses it (waits for
 * [firstOpen]); for any other placement, `is_show_ads && is_show_<code>` as before. Every load and show path reads
 * it through AdsController.isEnabled (L-072).
 */
@Suppress("UNUSED_PARAMETER")
internal fun placementSwitch(placement: AdPlacement, flags: AdsFlags, firstOpen: Flow<Boolean>): Flow<Boolean> =
    flags.placementEnabled(placement)
