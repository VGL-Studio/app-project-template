package com.vglstudio.template.core.config

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fetches the latest Remote Config values from the server and activates them (ADR-0018).
 * Only the loading screen may use this to block the UI; every other screen reads what is already there (ADR-0012).
 */
interface RemoteConfigRefresher {
    /** Returns true when fresh values are active. Never throws, except for cancellation. */
    suspend fun refresh(): Boolean
}

internal class RemoteConfigRefreshPolicy(
    private val isFakeConfig: () -> Boolean,
    private val fetchAndActivate: suspend () -> Unit,
    private val timeoutMillis: Long = REFRESH_TIMEOUT_MILLIS,
    private val onFailure: (Exception) -> Unit = {},
) {
    suspend fun refresh(): Boolean {
        if (isFakeConfig()) return true
        return try {
            withTimeoutOrNull(timeoutMillis) {
                fetchAndActivate()
                true
            } ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseRemoteConfigFetchThrottledException) {
            // The server answered a moment ago, so the active values are recent (ADR-0018).
            true
        } catch (e: Exception) {
            onFailure(e)
            false
        }
    }

    internal companion object {
        const val REFRESH_TIMEOUT_MILLIS = 10_000L
    }
}

/** Build variants that use the placeholder Firebase file have this project id (ADR-0012). */
private const val FAKE_PROJECT_ID = "fake-staging-project"
private const val TAG = "RemoteConfigRefresh"

internal class FirebaseRemoteConfigRefresher @Inject constructor() : RemoteConfigRefresher {
    private val policy = RemoteConfigRefreshPolicy(
        isFakeConfig = { FirebaseApp.getInstance().options.projectId == FAKE_PROJECT_ID },
        fetchAndActivate = {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            // Minimum interval 0: always ask the server, never the cache (ADR-0018).
            remoteConfig.fetch(0).await()
            remoteConfig.activate().await()
        },
        onFailure = { Log.w(TAG, "Could not refresh Remote Config", it) },
    )

    override suspend fun refresh(): Boolean = policy.refresh()
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    // A cancelled Task is a failed fetch, not a cancelled caller: report it as an error.
    addOnCanceledListener { continuation.resumeWithException(IOException("Remote Config task was cancelled")) }
}
