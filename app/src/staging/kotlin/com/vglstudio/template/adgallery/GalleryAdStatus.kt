package com.vglstudio.template.adgallery

import com.vglstudio.template.core.ads.AdsGlobalFlags
import com.vglstudio.template.core.ads.ConsentPath

// SKELETON (plan T-036, step 3): bodies come in step 4.

data class GalleryAdStatus(
    val consentPath: ConsentPath?,
    val adsAllowed: Boolean,
    val temporaryRight: Boolean,
    val privacyOptionsRequired: Boolean,
    val sdkReady: Boolean,
    val consentShortcutRemote: Boolean?,
    val nativeAfterInter: Boolean?,
    val nativeBottom: Boolean?,
    val nativeCollapsible: Boolean?,
) {
    companion object {
        val Unknown = GalleryAdStatus(null, false, false, false, false, null, null, null, null)
    }
}

fun galleryAdStatus(
    path: ConsentPath?,
    canRequestAds: Boolean,
    privacyOptionsRequired: Boolean,
    sdkReady: Boolean,
    switches: AdsGlobalFlags?,
): GalleryAdStatus = GalleryAdStatus.Unknown
