package com.vglstudio.template.core.ads

import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Skeleton of plan T-054 (step 3): the cover is not read yet.

internal fun inScreenAdsMayRun(shown: Boolean, covered: Boolean): Boolean = shown

internal fun inScreenAdWindowOf(visible: Flow<Boolean>, covered: Flow<Boolean>): Flow<Boolean> = visible

internal class InScreenAdWindow(private val logger: AppLogger) {
    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    fun onForeground(value: Boolean) {
        _open.value = value
    }

    fun onCovered(value: Boolean) = Unit
}
