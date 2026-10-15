package com.vglstudio.template.feature.loading

import androidx.lifecycle.ViewModel
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.config.FeatureFlags
import com.vglstudio.template.core.logging.AppLogger
import com.vglstudio.template.feature.loading.data.PreparationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Returns a monotonic time in milliseconds. A fun interface so tests can use virtual time. */
fun interface LoadingClock {
    fun elapsedRealtimeMillis(): Long
}

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val repository: PreparationRepository,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val featureFlags: FeatureFlags,
    private val clock: LoadingClock,
) : ViewModel() {
    private val state = MutableStateFlow<LoadingUiState>(LoadingUiState.InProgress(0f))
    val uiState: StateFlow<LoadingUiState> = state.asStateFlow()

    fun onRetry(): Unit = TODO()

    internal companion object {
        const val EVENT_COMPLETED = "loading_completed"
        const val MIN_DURATION_MILLIS = 2_000L
        const val LOG_TAG = "Loading"
    }
}
