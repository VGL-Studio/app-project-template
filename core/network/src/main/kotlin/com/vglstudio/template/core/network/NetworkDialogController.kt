package com.vglstudio.template.core.network

import com.vglstudio.template.core.analytics.Analytics
import com.vglstudio.template.core.config.RemoteSettings
import com.vglstudio.template.core.logging.AppLogger
import com.vglstudio.template.core.network.di.NetworkScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Skeleton for the tests written first (T-017, step 3): always hidden, records nothing. */
@Singleton
class NetworkDialogController @Inject internal constructor(
    private val connectivity: ConnectivitySource,
    private val settings: RemoteSettings,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val clock: NetworkClock,
    @param:NetworkScope private val scope: CoroutineScope,
) : NetworkStatus,
    NetworkFailureReporter {
    private val _state = MutableStateFlow<NetworkDialogState>(NetworkDialogState.Hidden)

    val state: StateFlow<NetworkDialogState> = _state.asStateFlow()

    override val dialogShown: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override val recovered: Flow<Unit> = emptyFlow()

    fun setScreen(screen: String?) = Unit

    fun setFullScreenAdShowing(showing: Boolean) = Unit

    fun onAppForeground() = Unit

    fun retry() = Unit

    override fun reportNetworkFailure(source: String) = Unit
}
