package com.vglstudio.template.feature.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.config.FeatureFlag
import com.vglstudio.template.core.config.FeatureFlags
import com.vglstudio.template.feature.loading.data.PreparationRepository
import com.vglstudio.template.feature.loading.data.PreparationTask
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Returns a monotonic time in milliseconds. A fun interface so tests can use virtual time. */
fun interface LoadingClock {
    fun elapsedRealtimeMillis(): Long
}

/**
 * Runs the four preparation tasks in parallel; each one done adds a quarter to the progress (spec: loading).
 * The screen stays at least [MIN_DURATION_MILLIS] and, if a task fails, waits for the user to retry.
 * The flag is read once when the screen opens: it only decides whether the app shows loading at start-up,
 * so watching it afterwards would mean nothing (plan T-001, question 3).
 */
@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val repository: PreparationRepository,
    private val analytics: Analytics,
    private val featureFlags: FeatureFlags,
    private val clock: LoadingClock,
) : ViewModel() {
    private val state = MutableStateFlow<LoadingUiState>(LoadingUiState.InProgress(0f))
    val uiState: StateFlow<LoadingUiState> = state.asStateFlow()

    private val doneTasks = mutableSetOf<PreparationTask>()
    private var startedAt = 0L
    private var retryCount = 0

    init {
        viewModelScope.launch {
            if (featureFlags.enabled(FeatureFlag.LOADING).first()) {
                startedAt = clock.elapsedRealtimeMillis()
                runPending()
            } else {
                state.value = LoadingUiState.Finished
            }
        }
    }

    fun onRetry() {
        if (state.value !is LoadingUiState.Failed) return
        retryCount++
        state.value = LoadingUiState.InProgress(progress())
        viewModelScope.launch { runPending() }
    }

    private suspend fun runPending() {
        coroutineScope {
            PreparationTask.entries.filter { it !in doneTasks }.map { task ->
                async {
                    if (repository.run(task)) {
                        doneTasks += task
                        state.value = LoadingUiState.InProgress(progress())
                    }
                }
            }.awaitAll()
        }
        if (doneTasks.size < PreparationTask.entries.size) {
            state.value = LoadingUiState.Failed(progress())
            return
        }
        val remaining = MIN_DURATION_MILLIS - (clock.elapsedRealtimeMillis() - startedAt)
        if (remaining > 0) delay(remaining)
        analytics.log(
            AnalyticsEvent(
                name = EVENT_COMPLETED,
                params = mapOf(
                    "duration_ms" to (clock.elapsedRealtimeMillis() - startedAt).toString(),
                    "retry_count" to retryCount.toString(),
                ),
            ),
        )
        state.value = LoadingUiState.Finished
    }

    private fun progress(): Float = doneTasks.size.toFloat() / PreparationTask.entries.size

    internal companion object {
        const val EVENT_COMPLETED = "loading_completed"
        const val MIN_DURATION_MILLIS = 2_000L
    }
}
