package com.vglstudio.template.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import com.vglstudio.template.core.logging.AppLogger

// Skeleton of plan T-091 so the tests written first compile; the rule comes with the code commit.

internal data class AnchoredAdMember(val seq: Int, val placement: AdPlacement, val allowed: Boolean?)

internal fun anchoredAdHolder(members: List<AnchoredAdMember>, holder: Int?): Int? = null

internal fun anchoredAdWaitLog(waiting: AdPlacement, holder: AdPlacement): String = ""

internal fun anchoredAdTurnLog(placement: AdPlacement): String = ""

internal fun isAnchoredFormat(format: AdFormat): Boolean = false

internal class AnchoredAdScope(@Suppress("unused") private val logger: AppLogger) {
    private var next = 0
    private val state = mutableStateOf<Int?>(null)
    val holder: Int? get() = state.value

    fun join(placement: AdPlacement): Int = ++next

    fun report(seq: Int, allowed: Boolean?) = Unit

    fun leave(seq: Int) = Unit
}

internal class AnchoredAdScopes(private val logger: AppLogger) {
    fun scopeFor(owner: LifecycleOwner): AnchoredAdScope = AnchoredAdScope(logger)

    fun containsForTest(owner: LifecycleOwner): Boolean = false

    val sizeForTest: Int get() = 0
}

@Composable
fun AnchoredAdScreen(content: @Composable () -> Unit) {
    content()
}
