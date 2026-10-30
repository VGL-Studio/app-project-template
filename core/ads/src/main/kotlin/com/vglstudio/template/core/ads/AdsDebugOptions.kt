package com.vglstudio.template.core.ads

import com.google.android.gms.ads.RequestConfiguration
import com.vglstudio.template.core.logging.AppLogger

// Skeleton of T-058, step 3: replaced by the code of step 4.
data class AdsDebugOptions(val testDeviceIds: List<String>, val logNetworkResponses: Boolean) {
    companion object {
        val NONE = AdsDebugOptions(emptyList(), false)

        @Suppress("UNUSED_PARAMETER")
        fun staging(rawTestDeviceIds: String): AdsDebugOptions = NONE
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun testDeviceIdsOf(raw: String): List<String> = emptyList()

@Suppress("UNUSED_PARAMETER")
internal fun requestConfigurationWithTestDevices(base: RequestConfiguration, ids: List<String>): RequestConfiguration? =
    null

@Suppress("UNUSED_PARAMETER")
internal fun applyTestDevices(
    options: AdsDebugOptions,
    current: () -> RequestConfiguration,
    set: (RequestConfiguration) -> Unit,
    logger: AppLogger,
) = Unit
