package com.vglstudio.template.core.ads

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FullScreenAdHistoryStoreTest {
    @Test
    fun emptyStore_readsUnknownTimes() = runTest {
        val dataStore = InMemoryDataStore()
        val store = DataStoreFullScreenAdHistoryStore(dataStore)
        // Control: a store that holds values reads them, so the empty read below is not a constant.
        DataStoreFullScreenAdHistoryStore(InMemoryDataStore()).let { filled ->
            filled.recordFirstOpenIfAbsent(1_000)
            assertEquals(1_000L, filled.read()?.firstOpenAtMillis)
        }
        assertEquals(FullScreenAdHistory(null, null), store.read())
    }

    @Test
    fun firstOpen_isWrittenOnce() = runTest {
        val store = DataStoreFullScreenAdHistoryStore(InMemoryDataStore())
        store.recordFirstOpenIfAbsent(1_000)
        store.recordFirstOpenIfAbsent(2_000)
        assertEquals(FullScreenAdHistory(firstOpenAtMillis = 1_000, lastShownAtMillis = null), store.read())
    }

    @Test
    fun lastShown_survivesNewStoreInstance() = runTest {
        val dataStore = InMemoryDataStore()
        DataStoreFullScreenAdHistoryStore(dataStore).recordShown(5_000)
        val reopened = DataStoreFullScreenAdHistoryStore(dataStore)
        assertEquals(FullScreenAdHistory(firstOpenAtMillis = null, lastShownAtMillis = 5_000), reopened.read())
    }

    @Test
    fun unreadableStorage_readsNull() = runTest {
        val store = DataStoreFullScreenAdHistoryStore(UnreadableDataStore())
        assertNull(store.read())
    }

    @Test
    fun unwritableStorage_throwsIOException() = runTest {
        val store = DataStoreFullScreenAdHistoryStore(UnreadableDataStore())
        val error = try {
            store.recordShown(5_000)
            null
        } catch (e: Exception) {
            e
        }
        if (error == null) fail("recordShown did not throw")
        assertTrue("Unexpected $error", error is IOException)
    }
}
