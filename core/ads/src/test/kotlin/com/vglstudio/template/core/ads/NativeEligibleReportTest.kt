package com.vglstudio.template.core.ads

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.analytics.AdFunnelEvent
import com.vglstudio.template.core.analytics.AdFunnelKind
import com.vglstudio.template.core.analytics.RecordingAdFunnel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `af_native_ad_eligible` (section 8.3; plan T-033): once each time a native slot that passed the app's gate comes into
 * the composition; a recomposition does not count, leaving and coming back does.
 */
@RunWith(AndroidJUnit4::class)
class NativeEligibleReportTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var reports = 0

    @Test
    fun enterOnce_recomposeDoesNotReport() {
        var label by mutableIntStateOf(0)
        composeRule.setContent {
            NativeEligibleEffect(allowed = true) { reports++ }
            Text("label $label")
        }
        composeRule.waitForIdle()
        assertEquals(1, reports)

        label = 1
        composeRule.waitForIdle()
        // Control: the screen did recompose.
        composeRule.onNodeWithText("label 1").assertExists()
        assertEquals(1, reports)
    }

    @Test
    fun leaveAndEnter_reportsAgain() {
        var present by mutableStateOf(true)
        composeRule.setContent {
            if (present) NativeEligibleEffect(allowed = true) { reports++ }
        }
        composeRule.waitForIdle()
        assertEquals(1, reports)

        present = false
        composeRule.waitForIdle()
        assertEquals(1, reports)
        present = true
        composeRule.waitForIdle()
        assertEquals(2, reports)
    }

    @Test
    fun notAllowed_reportsNothing_thenAllowed_reportsOnce() {
        var allowed by mutableStateOf(false)
        composeRule.setContent { NativeEligibleEffect(allowed = allowed) { reports++ } }
        composeRule.waitForIdle()
        assertEquals(0, reports)

        // Control (L-055): allowed, the same slot reports once.
        allowed = true
        composeRule.waitForIdle()
        assertEquals(1, reports)
    }

    @Test
    fun slotGate_withController_recordsNativeEligible() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        var allowed by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                NativeAdSlotGate(
                    allowed = allowed,
                    states = flowOf(NativeAdState.Loading),
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    onAdAvailabilityChange = {},
                    modifier = Modifier,
                    adContent = {},
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue(funnel.events.isEmpty())

        allowed = true
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }

    @Test
    fun slotGate_offScreen_recordsNothing_thenOnScreen_recordsOnce() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        var onScreen by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                NativeAdSlotGate(
                    allowed = true,
                    states = flowOf(NativeAdState.Loading),
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    onAdAvailabilityChange = {},
                    modifier = Modifier,
                    onScreen = onScreen,
                    adContent = {},
                )
            }
        }
        composeRule.waitForIdle()
        // A page of a pager kept in composition off screen has not come into view.
        assertTrue(funnel.events.isEmpty())

        onScreen = true
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }

    @Test
    fun fullPageStepGate_withController_recordsOnlyWhenShown() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        var shown by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                NativeFullScreenStepGate(
                    allowed = true,
                    states = flowOf(NativeAdState.Loading),
                    shown = shown,
                    adContent = {},
                )
            }
        }
        composeRule.waitForIdle()
        // A page of the flow kept but not on screen has not come into view.
        assertTrue(funnel.events.isEmpty())

        shown = true
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }
}
