package com.vglstudio.template.feature.loading

import androidx.lifecycle.ViewModel
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.config.FeatureFlags
import com.vglstudio.template.feature.loading.data.PreparationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Returns a monotonic time in milliseconds. A fun interface so tests can use virtual time. */
fun interface LoadingClock {
    fun elapsedRealtimeMillis(): Long
}

class LoadingViewModel(
    private val repository: PreparationRepository,
    private val analytics: Analytics,
    private val featureFlags: FeatureFlags,
    private val clock: LoadingClock,
) : ViewModel() {
    private val state = MutableStateFlow<LoadingUiState>(LoadingUiState.InProgress(0f))
    val uiState: StateFlow<LoadingUiState> = state.asStateFlow()

    init {
        TODO("T-001")
    }

    fun onRetry() {
        TODO("T-001")
    }

    internal companion object {
        const val EVENT_COMPLETED = "loading_completed"
        const val MIN_DURATION_MILLIS = 2_000L
    }
}
