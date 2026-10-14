package com.vglstudio.template.feature.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.config.FeatureFlag
import com.vglstudio.template.core.config.FeatureFlags
import com.vglstudio.template.feature.sample.data.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reference implementation of a feature ViewModel. New features follow this shape:
 * state exposed as a [StateFlow] of a sealed UI state, user actions as plain functions.
 */
@HiltViewModel
class SampleViewModel @Inject constructor(
    private val repository: CounterRepository,
    private val analytics: Analytics,
    featureFlags: FeatureFlags,
) : ViewModel() {
    private val enabled = featureFlags.isEnabled(FeatureFlag.SAMPLE)

    val uiState: StateFlow<SampleUiState> =
        (if (enabled) repository.count.map { SampleUiState.Ready(it) } else flowOf(SampleUiState.Disabled))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SampleUiState.Loading,
            )

    fun onIncrement() {
        if (!enabled) return
        viewModelScope.launch {
            repository.increment()
            analytics.log(AnalyticsEvent(name = EVENT_INCREMENT))
        }
    }

    internal companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val EVENT_INCREMENT = "sample_increment"
    }
}
