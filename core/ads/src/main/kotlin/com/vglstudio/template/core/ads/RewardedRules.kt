package com.vglstudio.template.core.ads

/**
 * What a rewarded placement does when no ad can be shown (MC-05): [Block] (the default, the user may try again),
 * [Allow] (the action runs), or [AllowAfter] (the action runs on the [AllowAfter.tries]-th try in a row with no ad).
 * Closing the ad before the reward always blocks, whatever the policy (R-02, R-04).
 */
sealed interface RewardedNoAdPolicy {
    /** The code of the policy, as a remote value would carry it: `block`, `allow`, `allow_after_<n>`. */
    val code: String

    data object Block : RewardedNoAdPolicy {
        override val code: String = "block"
    }

    data object Allow : RewardedNoAdPolicy {
        override val code: String = "allow"
    }

    data class AllowAfter(val tries: Int) : RewardedNoAdPolicy {
        override val code: String get() = "allow_after_$tries"
    }

    companion object {
        /** The policy of [code], or null when it is not one (question 28 of the ads spec). */
        fun parse(code: String): RewardedNoAdPolicy? = TODO("T-031")
    }
}

/** What one load round of a rewarded placement does (L-072). */
internal enum class RewardedLoadStep { NOT_NEEDED, SWITCHED_OFF, GATE_CLOSED, SEND }

/** The send decision of every rewarded load round, in this order: needed, switched on, consent gate open. */
internal fun rewardedLoadStep(needsLoad: Boolean, enabled: Boolean, mayRequest: Boolean): RewardedLoadStep =
    TODO("T-031")

/** Why no rewarded ad can be shown now (R-03, R-04). */
internal enum class RewardedNoAdReason { CONSENT_CLOSED, NETWORK, NOT_READY }

/** The consent gate first, then a ready ad, then the network: null when an ad can be shown. */
internal fun noAdReason(
    consentOpen: Boolean,
    ready: Boolean,
    offline: Boolean,
    lastLoadNetworkError: Boolean,
): RewardedNoAdReason? = TODO("T-031")

/** R-03: a load error is a network error with code 2, or with any code while the device is offline. */
internal fun isNetworkLoadError(code: Int, offline: Boolean): Boolean = TODO("T-031")

/** Whether [policy] lets the action run on a try with no ad, the [noAdInARow]-th one in a row (question 3). */
internal fun grantsWithoutAd(policy: RewardedNoAdPolicy, noAdInARow: Int): Boolean = TODO("T-031")

/** The loader of a full-screen slot in [AdsController]. */
internal enum class FullScreenLoader { INTERSTITIAL, NATIVE_FULL_SCREEN, APP_OPEN, REWARDED }

/** The loader of [format]; a format with no full-screen slot is an [IllegalArgumentException]. */
internal fun fullScreenLoaderOf(format: AdFormat): FullScreenLoader = TODO("T-031")
