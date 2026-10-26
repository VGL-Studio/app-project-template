package com.vglstudio.template.core.ads

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.analytics.RecordingAdMetrics
import com.vglstudio.template.core.logging.RecordingAppLogger
import com.vglstudio.template.core.network.InternetConnectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The entry point of features for full-screen ads, in tests and previews that provide no ads controller. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FullScreenAdsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bannerPlacement_isRejected() {
        val error = runCatching {
            composeRule.setContent { rememberShowFullScreenAd(AdPlacement.SAMPLE_NATIVE_SMALL) }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun withoutController_actionDoesNothing() {
        // No full-screen placement exists yet (T-014 adds one), so the action is built by the function that
        // rememberShowFullScreenAd wraps, with a recording show call in place of the controller's.
        val shown = mutableListOf<AdPlacement>()
        val show: suspend (AdsController, Activity, AdPlacement) -> Unit = { _, _, placement -> shown += placement }
        var withController by mutableStateOf(false)
        var action: () -> Unit = {}
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides if (withController) controller else null) {
                action = rememberFullScreenAdAction(AdPlacement.SAMPLE_NATIVE_SMALL, show)
                if (withController) Box(modifier = Modifier.size(1.dp).testTag(CONTROL))
                FullScreenAdHost()
            }
        }
        composeRule.waitForIdle()
        action()
        composeRule.waitForIdle()
        assertTrue(shown.isEmpty())
        // FullScreenAdHost draws nothing without a controller.
        composeRule.onAllNodes(isRoot().not()).assertCountEquals(0)

        // Control: with a controller, the same action shows the ad, and the node query sees drawn nodes.
        withController = true
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(CONTROL).assertCountEquals(1)
        action()
        composeRule.waitForIdle()
        assertEquals(listOf(AdPlacement.SAMPLE_NATIVE_SMALL), shown)
    }

    @Test
    fun showStartupAd_withoutController_finishesOnceWithFalse() {
        val results = mutableListOf<Boolean>()
        var action: (StartupAdTurn, (Boolean) -> Unit) -> Unit = { _, _ -> }
        composeRule.setContent { action = rememberShowStartupAd() }
        composeRule.waitForIdle()
        action(StartupAdTurn(AdPlacement.OPEN_APP)) { results += it }
        composeRule.waitForIdle()
        assertEquals(listOf(false), results)
    }

    @Test
    fun showFullScreenAd_rejectsAppWidePlacement() {
        var placement by mutableStateOf(AdPlacement.GALLERY_INTERSTITIAL)
        composeRule.setContent { rememberShowFullScreenAd(placement) }
        // Control: a full-screen placement that is not app-wide is accepted.
        composeRule.waitForIdle()
        val error = runCatching {
            placement = AdPlacement.OPEN_APP_INTER
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun rememberShowFullScreenAd_rejectsNativeCombo() {
        var placement by mutableStateOf(AdPlacement.GALLERY_NATIVE_FULL_SCREEN)
        composeRule.setContent { rememberShowFullScreenAd(placement) }
        // Control: a native full-screen placement that is not app-wide is accepted.
        composeRule.waitForIdle()
        val error = runCatching {
            placement = AdPlacement.NATIVE_AFTER_INTER
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    private val controller by lazy {
        AdsController(
            context = ApplicationProvider.getApplicationContext(),
            unitIds = GoogleTestAdUnitIds,
            consentDebug = ConsentDebugOptions.NONE,
            flags = DefaultAdsFlags(FakeRemoteSettings()),
            connectivity = object : InternetConnectivity {
                override val connected = flowOf(true)
            },
            shortcutConfig = ConsentShortcutConfig.OFF,
            metrics = RecordingAdMetrics(),
            logger = RecordingAppLogger(),
            firstOpenMark = FirstOpenMark(EmptyDataStore(), RecordingAppLogger()),
            interDisplayCounter = InterDisplayCounter(EmptyDataStore(), RecordingAppLogger()),
        )
    }

    private companion object {
        const val CONTROL = "control"
    }
}

/** A preferences store that is always empty and drops every write. */
private class EmptyDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flowOf(emptyPreferences())

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(emptyPreferences())
}
