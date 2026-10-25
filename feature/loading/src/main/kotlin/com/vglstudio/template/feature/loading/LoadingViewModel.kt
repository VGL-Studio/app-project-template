package com.vglstudio.template.feature.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.template.core.ads.StartupAdTurn
import com.vglstudio.template.core.ads.StartupAds
import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.analytics.AnalyticsEvent
import com.vglstudio.template.core.config.FeatureFlag
import com.vglstudio.template.core.config.FeatureFlags
import com.vglstudio.template.core.logging.AppLogger
import com.vglstudio.template.feature.loading.data.PreparationRepository
import com.vglstudio.template.feature.loading.data.PreparationTask
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
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
 * Once the tasks are done and the minimum time is over, the screen asks :core:ads for the startup ad (KB-13 of the
 * ads spec, plan T-024): with none it finishes, with one it stays at 100% until [LoadingRoute] has shown it.
 */
@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val repository: PreparationRepository,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val featureFlags: FeatureFlags,
    private val clock: LoadingClock,
    private val startupAds: StartupAds,
) : ViewModel() {
    private val state = MutableStateFlow<LoadingUiState>(LoadingUiState.InProgress(0f))
    val uiState: StateFlow<LoadingUiState> = state.asStateFlow()

    private val pending = MutableStateFlow<StartupAdTurn?>(null)

    /** The app open ad waiting to be shown by [LoadingRoute]; null when none. */
    val pendingAd: StateFlow<StartupAdTurn?> = pending.asStateFlow()

    /** Returns the waiting turn once, then null, so a recreated activity does not show it again. */
    fun takePendingAd(): StartupAdTurn? = pending.getAndUpdate { null }

    /**
     * The startup ad has closed, or nothing showed: the screen finishes. Any thread; a second call does nothing.
     * [interShown] is kept for the combo of T-026 and only logged today.
     */
    fun onAdFinished(interShown: Boolean) {
        if (adFinished.getAndSet(true)) return
        logger.info(LOG_TAG, "Startup ad finished, inter shown: $interShown")
        finish()
    }

    private val adFinished = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)

    private val doneTasks = mutableSetOf<PreparationTask>()
    private var startedAt = 0L
    private var retryCount = 0

    init {
        viewModelScope.launch {
            if (featureFlags.enabled(FeatureFlag.LOADING).first()) {
                startedAt = clock.elapsedRealtimeMillis()
                logger.info(LOG_TAG, "Loading started")
                runPending()
            } else {
                logger.info(LOG_TAG, "Loading is off, going to the start screen")
                state.value = LoadingUiState.Finished
            }
        }
    }

    fun onRetry() {
        if (state.value !is LoadingUiState.Failed) return
        retryCount++
        logger.info(LOG_TAG, "Retry $retryCount")
        state.value = LoadingUiState.InProgress(progress())
        viewModelScope.launch { runPending() }
    }

    private suspend fun runPending() {
        coroutineScope {
            PreparationTask.entries.filter { it !in doneTasks }.map { task ->
                async {
                    if (runSafely(task)) {
                        doneTasks += task
                        logger.info(LOG_TAG, "Task $task done")
                        state.value = LoadingUiState.InProgress(progress())
                    }
                }
            }.awaitAll()
        }
        if (doneTasks.size < PreparationTask.entries.size) {
            logger.warn(LOG_TAG, "${PreparationTask.entries.size - doneTasks.size} task(s) not done, waiting for retry")
            state.value = LoadingUiState.Failed(progress())
            return
        }
        val remaining = MIN_DURATION_MILLIS - (clock.elapsedRealtimeMillis() - startedAt)
        if (remaining > 0) delay(remaining)
        val turn = startupAd()
        if (turn == null) {
            finish()
        } else {
            // The screen stays at 100% while LoadingRoute shows the ad; onAdFinished then finishes it.
            pending.value = turn
        }
    }

    /** The startup ad of :core:ads, or null; an unexpected error is no ad, not a crash. */
    private suspend fun startupAd(): StartupAdTurn? = try {
        startupAds.awaitStartupAd()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn(LOG_TAG, "Startup ad failed, going on without it", e)
        null
    }

    /** Logs `loading_completed` once, at the move to the start screen (question 5 of plan T-024), then finishes. */
    private fun finish() {
        if (finished.getAndSet(true)) return
        val durationMillis = clock.elapsedRealtimeMillis() - startedAt
        analytics.log(
            AnalyticsEvent(
                name = EVENT_COMPLETED,
                params = mapOf(
                    "duration_ms" to durationMillis.toString(),
                    "retry_count" to retryCount.toString(),
                ),
            ),
        )
        logger.info(LOG_TAG, "Loading finished in $durationMillis ms after $retryCount retries")
        state.value = LoadingUiState.Finished
    }

    /** An unexpected error in one task is a failed task, not a crash of the whole app. */
    private suspend fun runSafely(task: PreparationTask): Boolean {
        val done = try {
            repository.run(task)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(LOG_TAG, "Task $task failed", e)
            return false
        }
        if (!done) logger.warn(LOG_TAG, "Task $task failed")
        return done
    }

    private fun progress(): Float = doneTasks.size.toFloat() / PreparationTask.entries.size

    internal companion object {
        const val EVENT_COMPLETED = "loading_completed"
        const val MIN_DURATION_MILLIS = 2_000L
        const val LOG_TAG = "Loading"
    }
}
