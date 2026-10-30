package com.vglstudio.template.core.ads

/** Skeleton for the tests of plan T-055 (step 3); the rules come in step 4. */
internal fun fullScreenHostUsable(finishing: Boolean, destroyed: Boolean, resumed: Boolean): Boolean = true

/** What the destruction of an app activity does to the full-screen turn on screen (plan T-055). */
internal enum class HostDestroyedAction { IGNORE, FORCE_COMPLETE, HAND_OVER }

/** Skeleton for the tests of plan T-055 (step 3). */
internal fun hostDestroyedAction(activityId: Int, hostId: Int?, changingConfigurations: Boolean): HostDestroyedAction =
    if (hostId != null && hostId == activityId) HostDestroyedAction.FORCE_COMPLETE else HostDestroyedAction.IGNORE
