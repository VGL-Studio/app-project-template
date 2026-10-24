package com.vglstudio.template.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The default preload plan and the checks of a plan (S-22, HD-02). */
class AdPreloadPlanTest {
    @Test
    fun default_putsAppOpenAndFirstStepFirst() {
        val plan = defaultPreloadPlan(
            listOf(AdFormat.INTERSTITIAL, AdFormat.APP_OPEN, AdFormat.NATIVE_SMALL, AdFormat.NATIVE_STEP),
        )

        assertEquals(
            listOf(
                setOf(AdFormat.APP_OPEN, AdFormat.NATIVE_STEP),
                setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_SMALL),
            ),
            plan.groups,
        )
    }

    @Test
    fun default_withoutAppOpenOrStep_isOneGroup() {
        val plan = defaultPreloadPlan(listOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN, AdFormat.INTERSTITIAL))

        assertEquals(listOf(setOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN)), plan.groups)
    }

    @Test
    fun fromPlacements_coversEveryTemplateFormat() {
        val plan = AdPreloadPlan.fromPlacements()

        assertEquals(AdPlacement.entries.map { it.format }.toSet(), plan.groups.flatten().toSet())
        assertEquals(plan.groups.sumOf { it.size }, plan.groups.flatten().toSet().size)
    }

    @Test
    fun formatInTwoGroups_isRejected() {
        val error = runCatching {
            AdPreloadPlan(listOf(setOf(AdFormat.INTERSTITIAL), setOf(AdFormat.NATIVE_SMALL, AdFormat.INTERSTITIAL)))
        }.exceptionOrNull()

        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun emptyGroups_areDropped() {
        val plan = AdPreloadPlan(listOf(emptySet(), setOf(AdFormat.INTERSTITIAL), emptySet()))

        assertEquals(listOf(setOf(AdFormat.INTERSTITIAL)), plan.groups)
    }
}
