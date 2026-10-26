package com.vglstudio.template.core.ads

import kotlinx.coroutines.flow.Flow

// SKELETON (T-033, step 3).
internal suspend fun awaitAttributionStart(
    sdkStarted: Flow<Boolean>,
    consentAnswered: Flow<Boolean>,
    consentFormOpen: Flow<Boolean>,
    waitMillis: Long,
) = Unit
