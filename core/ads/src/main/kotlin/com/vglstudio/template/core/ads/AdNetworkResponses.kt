package com.vglstudio.template.core.ads

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.vglstudio.template.core.logging.AppLogger

// Skeleton of T-058, step 3: replaced by the code of step 4.
internal data class NetworkResponse(val adapterClassName: String, val latencyMillis: Long, val errorCode: Int?)

@Suppress("UNUSED_PARAMETER")
internal fun networkResponsesOf(info: ResponseInfo?): List<NetworkResponse> = emptyList()

@Suppress("UNUSED_PARAMETER")
internal fun networkResponseLines(
    key: String,
    responses: List<NetworkResponse>,
    loadedAdapterClassName: String?,
): List<String> = emptyList()

@Suppress("UNUSED_PARAMETER", "unused")
internal class AdNetworkLog(private val enabled: Boolean, private val logger: AppLogger) {
    fun loaded(key: String, info: ResponseInfo?) = Unit

    fun failed(key: String, error: LoadAdError) = Unit

    companion object {
        val OFF = AdNetworkLog(false, NoLogger)
    }
}

private object NoLogger : AppLogger {
    override fun debug(tag: String, message: String) = Unit

    override fun info(tag: String, message: String) = Unit

    override fun warn(tag: String, message: String, error: Throwable?) = Unit

    override fun error(tag: String, message: String, error: Throwable?) = Unit
}
