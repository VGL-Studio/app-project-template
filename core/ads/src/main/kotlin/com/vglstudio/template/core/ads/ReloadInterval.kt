package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.StateFlow

/** The shortest reload interval of a slot, in seconds (KB-34, criterion 2). */
internal const val MIN_RELOAD_SECONDS = 30L

/** Skeleton (plan T-087, step 3 of auto-run.md): not written yet. */
internal fun reloadSeconds(raw: Long, defaultSeconds: Long): Long = defaultSeconds

/** Skeleton (plan T-087, step 3 of auto-run.md): not written yet. */
@Suppress("UNUSED_PARAMETER")
internal suspend fun awaitReloadTick(intervalMillis: StateFlow<Long>, extraMillis: Long = 0) = Unit
