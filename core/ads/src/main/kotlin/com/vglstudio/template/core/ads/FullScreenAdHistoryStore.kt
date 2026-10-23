package com.vglstudio.template.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject

internal interface FullScreenAdHistoryStore {
    suspend fun read(): FullScreenAdHistory?

    suspend fun recordFirstOpenIfAbsent(nowMillis: Long)

    suspend fun recordShown(nowMillis: Long)
}

// Stub for the test-first commit.
internal class DataStoreFullScreenAdHistoryStore @Inject constructor(private val dataStore: DataStore<Preferences>) :
    FullScreenAdHistoryStore {
    override suspend fun read(): FullScreenAdHistory? = FullScreenAdHistory(null, null)

    override suspend fun recordFirstOpenIfAbsent(nowMillis: Long) = Unit

    override suspend fun recordShown(nowMillis: Long) = Unit
}
