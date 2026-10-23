package com.vglstudio.template.core.assets

import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Why a file could not be loaded, so callers can react differently (for example show a network dialog). */
enum class AssetFailureReason {
    /** The device could not reach the server. */
    NETWORK,

    /** The server answered that the file does not exist (HTTP 404). */
    NOT_FOUND,

    /** Any other failure. */
    OTHER,
}

private const val MAX_CAUSE_DEPTH = 10

/** Looks at the error and its causes: a connection problem is [AssetFailureReason.NETWORK], HTTP 404 is not found. */
internal fun IOException.toFailureReason(): AssetFailureReason {
    var error: Throwable? = this
    var depth = 0
    while (error != null && depth < MAX_CAUSE_DEPTH) {
        when (error) {
            is HttpStatusException ->
                return if (error.status == HTTP_NOT_FOUND) AssetFailureReason.NOT_FOUND else AssetFailureReason.OTHER
            is UnknownHostException, is SocketException, is SocketTimeoutException -> return AssetFailureReason.NETWORK
        }
        error = error.cause
        depth++
    }
    return AssetFailureReason.OTHER
}

private const val HTTP_NOT_FOUND = 404

/**
 * The log line for a failed download: the reason and the error class, because the stack trace of
 * an UnknownHostException is often empty in logcat. The error message (it may hold the host) is left out.
 */
internal fun downloadFailureMessage(path: AssetPath, error: IOException): String =
    "Could not download ${path.value}: ${error.toFailureReason()} (${error::class.java.simpleName})"
