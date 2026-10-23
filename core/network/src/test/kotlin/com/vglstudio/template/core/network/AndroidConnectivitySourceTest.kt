package com.vglstudio.template.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

/** Robolectric's default network has no VALIDATED capability, so it is not usable Internet. */
@RunWith(AndroidJUnit4::class)
class AndroidConnectivitySourceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    private fun capabilities(vararg capabilities: Int): NetworkCapabilities {
        val result = ShadowNetworkCapabilities.newInstance()
        val shadow = shadowOf(result)
        shadow.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        capabilities.forEach { shadow.addCapability(it) }
        return result
    }

    @Test
    fun wifiWithInternetNotValidated_isNotUsable() {
        val wifi = capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        assertFalse(wifi.hasUsableInternet())

        shadowOf(wifi).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        assertTrue(wifi.hasUsableInternet())

        assertFalse((null as NetworkCapabilities?).hasUsableInternet())
    }

    @Test
    fun isUsableInternet_needsBoth() {
        assertTrue(isUsableInternet(hasInternet = true, isValidated = true))
        assertFalse(isUsableInternet(hasInternet = true, isValidated = false))
        assertFalse(isUsableInternet(hasInternet = false, isValidated = true))
        assertFalse(isUsableInternet(hasInternet = false, isValidated = false))
    }

    @Test
    fun callback_reportsCapabilitiesAndLoss_thenUnregisters() = runTest {
        val source = AndroidConnectivitySource(context)
        val network = manager.activeNetwork!!
        assertTrue(shadowOf(manager).networkCallbacks.isEmpty())

        source.connectivity().test {
            assertFalse(awaitItem())
            val callbacks = shadowOf(manager).networkCallbacks
            assertEquals(1, callbacks.size)
            val callback = callbacks.single()

            callback.onCapabilitiesChanged(network, capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            callback.onCapabilitiesChanged(
                network,
                capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET, NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            )
            assertTrue(awaitItem())

            callback.onLost(network)
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(shadowOf(manager).networkCallbacks.isEmpty())
    }

    @Test
    fun isConnected_readsActiveNetworkCapabilities() {
        val source = AndroidConnectivitySource(context)
        val network = manager.activeNetwork!!
        assertFalse(source.isConnected())

        shadowOf(manager).setNetworkCapabilities(
            network,
            capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET, NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        )
        assertTrue(source.isConnected())

        shadowOf(manager).setNetworkCapabilities(network, capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        assertFalse(source.isConnected())
    }
}
