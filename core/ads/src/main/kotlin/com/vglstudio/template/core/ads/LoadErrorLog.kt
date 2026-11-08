package com.vglstudio.template.core.ads

/**
 * The SDK message of a load error in the load error line of staging (plan T-093, KB-28). Skeleton of the tests first
 * step: no message yet.
 */
internal class LoadErrorLog(private val withMessage: Boolean) {
    fun messageOf(sdkMessage: String?): String? = null

    fun text(code: Int, sdkMessage: String?): String = loadErrorText(code, messageOf(sdkMessage))

    companion object {
        /** No message: the line of production. */
        val CODE_ONLY = LoadErrorLog(false)
    }
}

internal const val AD_UNIT_MASK = "<ad unit>"

internal const val MAX_MESSAGE_CHARS = 300

internal fun loadErrorText(code: Int, logMessage: String?): String = "$code"

internal fun logSafeMessage(message: String): String = message
