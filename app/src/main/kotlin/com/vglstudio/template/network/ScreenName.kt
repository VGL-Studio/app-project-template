package com.vglstudio.template.network

private const val UNKNOWN = "unknown"
private const val LOADING = "loading"

// Firebase keeps at most 100 characters of a parameter value.
private const val MAX_LENGTH = 100

private val wordStart = Regex("(?<=[a-z0-9])(?=[A-Z])")

/**
 * The `screen` parameter of network_lost_shown (spec network, section 6), from the route of a `@Serializable`
 * destination: arguments dropped, the class name without its package and `Destination` suffix, in snake_case.
 * `com.x.SampleDestination` gives `sample`, `com.x.AdGalleryDestination/{id}` gives `ad_gallery`.
 */
internal fun screenNameOf(route: String?): String {
    val name = route.orEmpty()
        .substringBefore('?')
        .substringBefore('/')
        .substringAfterLast('.')
        .removeSuffix("Destination")
    if (name.isEmpty()) return UNKNOWN
    return name.replace(wordStart, "_").lowercase().take(MAX_LENGTH)
}

/**
 * The screen :app reports to the network dialog: `null` while nothing is on the back stack yet (the first frame) and
 * on the loading screen, so the dialog never shows over loading (spec network, KB-1); otherwise [screenNameOf].
 */
internal fun reportedScreenOf(route: String?): String? {
    if (route == null) return null
    return screenNameOf(route).takeUnless { it == LOADING }
}

/** Skeleton for the tests written first (T-017, review fix): reports every route, as before the fix. */
internal fun reportsScreen(route: String?): Boolean = true
