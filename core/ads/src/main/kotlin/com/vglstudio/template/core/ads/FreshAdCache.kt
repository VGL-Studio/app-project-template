package com.vglstudio.template.core.ads

/** Preloaded ads per placement, each usable for [maxAgeMillis] after it was loaded. */
internal class FreshAdCache<T>(private val maxAgeMillis: Long = MAX_AGE_MILLIS, private val onDiscard: (T) -> Unit) {
    fun put(key: AdPlacement, ad: T, nowMillis: Long) = Unit

    fun hasFresh(key: AdPlacement, nowMillis: Long): Boolean = false

    fun take(key: AdPlacement, nowMillis: Long): T? = null

    fun contains(key: AdPlacement): Boolean = false

    companion object {
        const val MAX_AGE_MILLIS = 60 * 60 * 1_000L
    }
}
