package com.vglstudio.template.network

private const val UNKNOWN = "unknown"
private const val LOADING = "loading"

// Firebase keeps at most 100 characters of a parameter value.
private const val MAX_LENGTH = 100

private val wordStart = Regex("(?<=[a-z0-9])(?=[A-Z])")

/**
 * The `screen` parameter of network_lost_shown (spec network, section 6), from the route of a `@Serializable`
 * destination: arguments dropped, the class name without its package and `Destination` suffix, in snake_case.
 * `com.x.SampleDestination` gives `sample`, `com.x.PhotoDetailDestination/{id}` gives `photo_detail`.
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

/**
 * Whether :app reports [route] to the network dialog. Not while the back stack has no entry yet (`null` route): on a
 * cold start the controller already starts as on loading, and after the activity is recreated (rotation, dark mode,
 * locale) the first frame has no entry before the restored destination comes back. Reporting `null` there would
 * look like a passage through loading and drop the dialog shown before the recreation.
 */
internal fun reportsScreen(route: String?): Boolean = route != null
