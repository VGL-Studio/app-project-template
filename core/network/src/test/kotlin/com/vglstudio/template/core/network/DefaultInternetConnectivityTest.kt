package com.vglstudio.template.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The shared Internet watch for other core modules replays the system source (S-09, ADR-0024). */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInternetConnectivityTest {
    @Test
    fun followsSourceConnectivity() = runTest {
        val source = FakeConnectivitySource(connected = false)
        val connectivity = DefaultInternetConnectivity(source)
        val values = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { connectivity.connected.toList(values) }
        source.connected.value = true
        assertEquals(listOf(false, true), values)
    }
}
