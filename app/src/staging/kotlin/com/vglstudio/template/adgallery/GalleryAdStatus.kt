package com.vglstudio.template.adgallery

import com.vglstudio.template.core.ads.AdsGlobalFlags
import com.vglstudio.template.core.ads.ConsentPath

/**
 * What the "Ad status" page of the staging ad gallery shows, for testers checking KB-11 (plan T-036, step 5): how ads
 * were allowed in this process, whether they are allowed now and only for now, and the remote ad switches. A switch is
 * null until its first value (shown as pending).
 */
data class GalleryAdStatus(
    /** How ads were allowed in this process (S-13); null before the first result. */
    val consentPath: ConsentPath?,
    /** Ads may be requested right now (consent gate open and SDK started). */
    val adsAllowed: Boolean,
    /** [adsAllowed] only for now: allowed on the early cached path or after a consent error (ConsentGate.kt). */
    val temporaryRight: Boolean,
    val privacyOptionsRequired: Boolean,
    val sdkReady: Boolean,
    /** `is_enable_consent_shortcut`. */
    val consentShortcutRemote: Boolean?,
    /** `is_show_ads && is_show_native_after_inter`: the combo after the interstitial button (T-026). */
    val nativeAfterInter: Boolean?,
    /** `is_show_native_bottom` (T-030). */
    val nativeBottom: Boolean?,
    /** `is_show_native_bottom && is_show_native_collap` (T-030). */
    val nativeCollapsible: Boolean?,
) {
    companion object {
        /** No ads controller, or nothing known yet. */
        val Unknown = GalleryAdStatus(
            consentPath = null,
            adsAllowed = false,
            temporaryRight = false,
            privacyOptionsRequired = false,
            sdkReady = false,
            consentShortcutRemote = null,
            nativeAfterInter = null,
            nativeBottom = null,
            nativeCollapsible = null,
        )
    }
}

/** The two consent paths that open the gate only for now: `temporary = true` in ConsentGate.kt. */
private val TEMPORARY_PATHS = setOf(ConsentPath.CACHED_EARLY, ConsentPath.ERROR_ALLOWED)

/**
 * The status of the gallery from the public flows of the ads controller and the global switches ([switches] null
 * until their first value). A right is temporary while ads are allowed on one of the two paths of [TEMPORARY_PATHS]; it
 * changes no consent rule.
 */
fun galleryAdStatus(
    path: ConsentPath?,
    canRequestAds: Boolean,
    privacyOptionsRequired: Boolean,
    sdkReady: Boolean,
    switches: AdsGlobalFlags?,
): GalleryAdStatus = GalleryAdStatus(
    consentPath = path,
    adsAllowed = canRequestAds,
    temporaryRight = canRequestAds && path in TEMPORARY_PATHS,
    privacyOptionsRequired = privacyOptionsRequired,
    sdkReady = sdkReady,
    consentShortcutRemote = switches?.consentShortcutEnabled,
    nativeAfterInter = switches?.showNativeAfterInter,
    nativeBottom = switches?.showNativeBottom,
    nativeCollapsible = switches?.showNativeCollap,
)
