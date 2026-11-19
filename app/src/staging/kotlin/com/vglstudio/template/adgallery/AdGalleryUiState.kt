package com.vglstudio.template.adgallery

/** The page of the staging ad gallery on screen (plan T-014). */
sealed interface AdGalleryUiState {
    /** Native small and medium, the full-screen buttons and Ad Inspector. */
    data object Default : AdGalleryUiState

    /** A list of [itemCount] items with native ads inside it. */
    data class InList(val itemCount: Int) : AdGalleryUiState

    /** A list of [itemCount] items with a collapsible native ad pinned at the bottom. */
    data class Collapsible(val itemCount: Int) : AdGalleryUiState

    /** A fixed banner on top, the MREC banner under it and a list of [itemCount] items (T-029, KB-29). */
    data class Banners(val itemCount: Int) : AdGalleryUiState

    /**
     * A list of [itemCount] items with the collapsible banner at the bottom (B-03): a page of its own, one anchored ad
     * per page (MC-12, plan T-091).
     */
    data class CollapsibleBanner(val itemCount: Int) : AdGalleryUiState

    /** A grid of [itemCount] items with native cards inside it (T-028). */
    data class Grid(val itemCount: Int) : AdGalleryUiState

    /** A pager of [pageCount] pages, one of them a native card (T-028). */
    data class Pager(val pageCount: Int) : AdGalleryUiState

    /** A flow of [stepCount] steps with the native card of a step and the native page of a step (T-032). */
    data class FlowSteps(val stepCount: Int) : AdGalleryUiState

    /** The sample action held behind the rewarded ad, [policy] when no ad, run [actionsRun] times (T-031). */
    data class Rewarded(val policy: GalleryRewarded, val actionsRun: Int) : AdGalleryUiState

    /** The consent path, the temporary right and the remote ad switches (KB-11). */
    data object Status : AdGalleryUiState
}

/** The full-screen ad a gallery button asks for. */
enum class GalleryFullScreen { INTERSTITIAL, NATIVE_FULL_SCREEN, APP_OPEN }

/**
 * What the action of `gallery_rw_action` does when no ad can be shown, picked on the rewarded page: block, allow,
 * or allow after 2 tries in a row (question 13 of the ads spec).
 */
enum class GalleryRewarded { BLOCK, ALLOW, ALLOW_AFTER }
