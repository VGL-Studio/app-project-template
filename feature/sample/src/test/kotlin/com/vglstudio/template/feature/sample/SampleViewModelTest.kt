package com.vglstudio.template.feature.sample

import app.cash.turbine.test
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.config.FeatureFlag
import com.vglstudio.template.core.config.FeatureFlags
import com.vglstudio.template.feature.sample.data.CounterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SampleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCounterRepository()
    private val analytics = FakeAnalytics()

    private fun viewModel(enabled: Boolean = true) = SampleViewModel(
        repository = repository,
        analytics = analytics,
        featureFlags = FakeFeatureFlags(enabled),
    )

    @Test
    fun whenStarted_thenShowsStoredCount() = runTest {
        repository.counter.value = 4
        viewModel().uiState.test {
            assertEquals(SampleUiState.Ready(4), awaitItemAfterLoading())
        }
    }

    @Test
    fun whenIncrement_thenCountGoesUpAndEventIsLogged() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SampleUiState.Ready(0), awaitItemAfterLoading())
            viewModel.onIncrement()
            assertEquals(SampleUiState.Ready(1), awaitItem())
        }
        assertEquals(listOf(AnalyticsEvent(SampleViewModel.EVENT_INCREMENT)), analytics.events)
    }

    @Test
    fun whenFlagIsOff_thenScreenIsDisabledAndIncrementDoesNothing() = runTest {
        val viewModel = viewModel(enabled = false)
        viewModel.uiState.test {
            assertEquals(SampleUiState.Disabled, awaitItemAfterLoading())
        }
        viewModel.onIncrement()
        assertEquals(0, repository.counter.value)
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)
    }
}

private suspend fun app.cash.turbine.ReceiveTurbine<SampleUiState>.awaitItemAfterLoading(): SampleUiState {
    val first = awaitItem()
    return if (first == SampleUiState.Loading) awaitItem() else first
}

private class FakeCounterRepository : CounterRepository {
    val counter = MutableStateFlow(0)
    override val count: StateFlow<Int> = counter

    override suspend fun increment() {
        counter.value += 1
    }
}

private class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}

private class FakeFeatureFlags(private val enabled: Boolean) : FeatureFlags {
    override fun isEnabled(flag: FeatureFlag): Boolean = enabled
}
