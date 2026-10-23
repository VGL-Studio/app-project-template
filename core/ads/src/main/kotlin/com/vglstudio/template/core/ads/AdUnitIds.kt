package com.vglstudio.template.core.ads

/**
 * The ad unit of each placement for the current build, bound in :app per build flavor (ADR-0007):
 * [GoogleTestAdUnitIds] in staging, [ConfiguredAdUnitIds] in production. `null` means the placement shows no ad.
 */
fun interface AdUnitIds {
    fun of(placement: AdPlacement): String?
}

/** Google's public test ad units. They never produce real impressions, so tapping them is safe. */
object GoogleTestAdUnitIds : AdUnitIds {
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"

    override fun of(placement: AdPlacement): String = unitFor(placement.format)

    /** The test unit of each format; also checks formats that have no placement yet. */
    internal fun unitFor(format: AdFormat): String = when (format) {
        AdFormat.BANNER -> "ca-app-pub-3940256099942544/9214589741"
        AdFormat.INTERSTITIAL -> "ca-app-pub-3940256099942544/1033173712"
        AdFormat.NATIVE_SMALL -> "ca-app-pub-3940256099942544/2247696110"
        // Native video, so the media view can be tried with a video.
        AdFormat.NATIVE_MEDIUM -> "ca-app-pub-3940256099942544/1044960115"
        // Opens as medium, so it uses the same native video unit.
        AdFormat.NATIVE_COLLAPSIBLE -> "ca-app-pub-3940256099942544/1044960115"
        AdFormat.NATIVE_FULL_SCREEN -> ""
    }
}

/**
 * Real ad units, given when the release is built and never stored in the repo (ADR-0009).
 * [config] has the form `<placement key>=<ad unit>;<placement key>=<ad unit>`.
 */
class ConfiguredAdUnitIds(config: String) : AdUnitIds {
    private val units: Map<String, String> = config.split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .associate { entry ->
            val key = entry.substringBefore('=').trim()
            val unit = entry.substringAfter('=', missingDelimiterValue = "").trim()
            require(key.isNotEmpty() && unit.isNotEmpty()) { "Ad unit entry must be <placement key>=<ad unit>" }
            key to unit
        }

    /** Placements that have no ad unit in this build. A release must have none. */
    val missing: List<AdPlacement> = AdPlacement.entries.filter { it.key !in units }

    override fun of(placement: AdPlacement): String? = units[placement.key]
}
