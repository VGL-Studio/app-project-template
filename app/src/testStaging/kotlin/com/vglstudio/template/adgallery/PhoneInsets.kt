package com.vglstudio.template.adgallery

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * The frame of a usual phone in landscape (plan T-049, FB-20261028-01): 2400 x 1080 px at 440 dpi, like the Redmi
 * Note 12 the page was found empty on. Give it the system insets of that phone with [dispatchPhoneLandscapeInsets].
 */
internal const val PHONE_LANDSCAPE = "w873dp-h393dp-440dpi"

/** The status bar of the phone in landscape, in px. */
internal const val PHONE_STATUS_BAR_PX = 94

/** The camera cutout on the left of the phone in landscape, in px. */
internal const val PHONE_CUTOUT_LEFT_PX = 94

/** The gesture navigation bar at the bottom of the phone in landscape, in px. */
internal const val PHONE_NAVIGATION_BAR_PX = 44

/**
 * Sends the system insets of the phone in landscape (status bar, camera cutout on the left, navigation bar) once to
 * the window of [rule], as the device does. Call it after `setContent`; content in `safeDrawingPadding()`, as
 * `AdGalleryActivity` puts the gallery, then gets the page area of the device to the pixel.
 */
internal fun dispatchPhoneLandscapeInsets(rule: ComposeContentTestRule) {
    val activity = (rule as AndroidComposeTestRule<*, *>).activity
    val insets = WindowInsetsCompat.Builder()
        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, PHONE_STATUS_BAR_PX, 0, 0))
        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, PHONE_NAVIGATION_BAR_PX))
        .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(PHONE_CUTOUT_LEFT_PX, 0, 0, 0))
        .build()
    rule.runOnUiThread { ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets) }
    rule.waitForIdle()
}
