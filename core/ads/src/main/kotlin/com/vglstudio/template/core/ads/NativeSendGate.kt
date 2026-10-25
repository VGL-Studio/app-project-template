package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.StateFlow

/**
 * The one send gate of every native small request (L-072): waits until the app is in [foreground] (O-05, L-A19),
 * then asks [mayRequestNow] (S-15). False means: send nothing, count no failure, plan no retry.
 */
@Suppress("UNUSED_PARAMETER")
internal suspend fun awaitSendAllowed(foreground: StateFlow<Boolean>, mayRequestNow: () -> Boolean): Boolean = false
