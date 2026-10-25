package com.vglstudio.template.feature.loading

import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.StartupAdTurn
import com.vglstudio.template.core.ads.StartupAds
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.config.FeatureFlag
import com.vglstudio.template.core.config.FeatureFlags
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.logging.RecordingAppLogger
import com.vglstudio.template.feature.loading.data.PreparationRepository
import com.vglstudio.template.feature.loading.data.PreparationTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * [LoadingRoute] in a real activity, recreated while the startup ad shows (L-061), finished from a background
 * thread (L-051). The [LoadingViewModel] is shared by every activity of a test, as the ViewModel store keeps it
 * across a recreation. No MainDispatcherRule: viewModelScope runs on the main looper of Robolectric, and
 * [SystemClock] moves with it.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LoadingRouteTest {
    @After
    fun tearDown() {
        RouteHost.reset()
    }

    private fun open(turn: StartupAdTurn?): ActivityController<RouteTestActivity> {
        RouteHost.viewModel = LoadingViewModel(
            repository = InstantPreparationRepository(),
            analytics = NoAnalytics(),
            logger = RecordingAppLogger(),
            featureFlags = LoadingOn(),
            clock = { SystemClock.elapsedRealtime() },
            startupAds = ImmediateStartupAds(turn),
        )
        val controller = Robolectric.buildActivity(RouteTestActivity::class.java).setup()
        idle()
        return controller
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
    }

    @Test
    fun pendingAd_isShownOnce_thenNavigatesAfterFinish() {
        // Control: with no startup ad, nothing shows and the screen moves on by itself.
        open(turn = null)
        assertEquals(0, RouteHost.shown.size)
        assertEquals(1, RouteHost.navigations)

        RouteHost.reset()
        open(turn = StartupAdTurn(AdPlacement.OPEN_APP))
        assertEquals(listOf(AdPlacement.OPEN_APP), RouteHost.shown.map { it.placement })
        assertEquals(0, RouteHost.navigations)

        RouteHost.finishers.single()(false)
        idle()
        assertEquals(1, RouteHost.navigations)
        idle()
        assertEquals(listOf(AdPlacement.OPEN_APP), RouteHost.shown.map { it.placement })
    }

    @Test
    fun recreateWhileAdShows_doesNotShowAgain() {
        val controller = open(turn = StartupAdTurn(AdPlacement.OPEN_APP))
        assertEquals(1, RouteHost.shown.size)
        val first = controller.get()

        controller.recreate()
        idle()
        // Control: the activity was really made again.
        check(controller.get() !== first)
        assertEquals(1, RouteHost.shown.size)
        assertEquals(0, RouteHost.navigations)

        // The action kept from the first activity ends the turn on the new one.
        RouteHost.finishers.single()(false)
        idle()
        assertEquals(1, RouteHost.navigations)
        assertEquals(1, RouteHost.shown.size)
    }

    @Test
    fun finishFromBackgroundThread_navigatesOnMainThread() {
        open(turn = StartupAdTurn(AdPlacement.OPEN_APP))
        assertEquals(1, RouteHost.shown.size)

        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit { RouteHost.finishers.single()(false) }.get(5, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }
        idle()
        assertEquals(1, RouteHost.navigations)
        assertSame(Looper.getMainLooper(), RouteHost.navigationLoopers.single())
    }
}

/** What the activities of [LoadingRouteTest] share; reset after each test. */
private object RouteHost {
    lateinit var viewModel: LoadingViewModel
    val shown = mutableListOf<StartupAdTurn>()
    val finishers = mutableListOf<(Boolean) -> Unit>()
    var navigations = 0
    val navigationLoopers = mutableListOf<Looper?>()

    fun showAd(turn: StartupAdTurn, onFinished: (Boolean) -> Unit) {
        shown += turn
        finishers += onFinished
    }

    fun onFinished() {
        navigations++
        navigationLoopers += Looper.myLooper()
    }

    fun reset() {
        shown.clear()
        finishers.clear()
        navigations = 0
        navigationLoopers.clear()
    }
}

/** Not in the manifest: Robolectric builds it all the same (as AdsActivityTrackerTest of :core:ads). */
class RouteTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                LoadingRoute(
                    onFinished = RouteHost::onFinished,
                    viewModel = RouteHost.viewModel,
                    showAd = RouteHost::showAd,
                )
            }
        }
    }
}

private class InstantPreparationRepository : PreparationRepository {
    override suspend fun run(task: PreparationTask): Boolean = true
}

private class NoAnalytics : Analytics {
    override fun log(event: AnalyticsEvent) = Unit
}

private class LoadingOn : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> = flowOf(true)
}

private class ImmediateStartupAds(private val turn: StartupAdTurn?) : StartupAds {
    override suspend fun awaitStartupAd(): StartupAdTurn? = turn
}
