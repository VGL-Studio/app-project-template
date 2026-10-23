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

/** Enough to tell the cause apart, short enough for one logcat line. */
private const val MAX_LOGGED_CLASSES = 5

/**
 * The log line for a failed download: the reason, the error classes along the cause chain ("IOException <-
 * UnknownHostException") and the HTTP status if any, because the stack trace of an UnknownHostException is often
 * empty in logcat and a wrapped error hides its cause. The error messages are left out: they may hold the address.
 */
internal fun downloadFailureMessage(path: AssetPath, error: IOException): String {
    val chain = generateSequence<Throwable>(error) { it.cause }.take(MAX_LOGGED_CLASSES).toList()
    val classes = chain.joinToString(" <- ") { it.javaClass.simpleName.ifEmpty { it.javaClass.name } }
    val status = chain.firstNotNullOfOrNull { it as? HttpStatusException }?.status
    val statusPart = if (status != null) ", status=$status" else ""
    return "Could not download ${path.value}: ${error.toFailureReason()} ($classes$statusPart)"
}
