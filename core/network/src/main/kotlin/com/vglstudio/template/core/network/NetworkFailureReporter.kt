package com.vglstudio.template.core.network

/**
 * Called by a module that saw a download fail because of the network (for example :core:assets), so the network
 * dialog can show at once (spec network, KB-3). [source] is a short label of the caller, logged only; never a path
 * or user data.
 */
fun interface NetworkFailureReporter {
    fun reportNetworkFailure(source: String)
}
