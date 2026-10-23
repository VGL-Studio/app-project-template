package com.vglstudio.template.adgallery

/** The page of the staging ad gallery on screen (plan T-014). */
sealed interface AdGalleryUiState {
    /** Native small and medium, the full-screen buttons and Ad Inspector. */
    data object Default : AdGalleryUiState

    /** A list of [itemCount] items with native ads inside it. */
    data class InList(val itemCount: Int) : AdGalleryUiState

    /** A list of [itemCount] items with a collapsible native ad pinned at the bottom. */
    data class Collapsible(val itemCount: Int) : AdGalleryUiState
}

/** The full-screen ad a gallery button asks for. */
enum class GalleryFullScreen { INTERSTITIAL, NATIVE_FULL_SCREEN }
