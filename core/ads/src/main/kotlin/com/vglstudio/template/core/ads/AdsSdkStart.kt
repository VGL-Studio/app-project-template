package com.vglstudio.template.core.ads

// Skeleton of plan T-068, step 3: the wrong order on purpose, until step 4.
internal suspend fun startAdsSdks(
    initAdmob: suspend () -> Unit,
    awaitAdmobReady: suspend () -> Unit,
    startMax: () -> Unit,
    preload: () -> Unit,
) {
    initAdmob()
    startMax()
    awaitAdmobReady()
    preload()
}
