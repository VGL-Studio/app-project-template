package com.vglstudio.template.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** Wall clock and monotonic clock moved by hand. */
internal class FakeAdClock(var wallMillis: Long = 0, var elapsedMillis: Long = 0) : AdClock {
    override fun wallClockMillis(): Long = wallMillis

    override fun elapsedRealtimeMillis(): Long = elapsedMillis

    fun advanceBy(millis: Long) {
        wallMillis += millis
        elapsedMillis += millis
    }
}

/** History store kept in memory; reads and writes can be made to fail like a broken disk. */
internal class InMemoryFullScreenAdHistoryStore(
    var firstOpenAtMillis: Long? = null,
    var lastShownAtMillis: Long? = null,
) : FullScreenAdHistoryStore {
    var failReads = false
    var failWrites = false

    override suspend fun read(): FullScreenAdHistory? =
        if (failReads) null else FullScreenAdHistory(firstOpenAtMillis, lastShownAtMillis)

    override suspend fun recordFirstOpenIfAbsent(nowMillis: Long) {
        if (failWrites) throw IOException("disk write failed")
        if (firstOpenAtMillis == null) firstOpenAtMillis = nowMillis
    }

    override suspend fun recordShown(nowMillis: Long) {
        if (failWrites) throw IOException("disk write failed")
        lastShownAtMillis = nowMillis
    }
}

internal class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** A preferences file that can be neither read nor written (I/O error). */
internal class UnreadableDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("disk read failed") }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("disk write failed")
}
