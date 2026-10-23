package com.vglstudio.template.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Wall-clock times of full-screen ads kept on the device, so the rules hold across app restarts (ADR-0022). */
internal interface FullScreenAdHistoryStore {
    /** Returns `null` when the storage cannot be read. */
    suspend fun read(): FullScreenAdHistory?

    /** Saves the first time the app was opened, unless one is saved already. Throws [IOException]. */
    suspend fun recordFirstOpenIfAbsent(nowMillis: Long)

    /** Saves the time of the last full-screen ad. Throws [IOException]. */
    suspend fun recordShown(nowMillis: Long)
}

/** Uses the app's shared preferences file of :core:data; keys start with `ads_` so they never clash. */
internal class DataStoreFullScreenAdHistoryStore @Inject constructor(private val dataStore: DataStore<Preferences>) :
    FullScreenAdHistoryStore {
    override suspend fun read(): FullScreenAdHistory? = try {
        val preferences = dataStore.data.first()
        FullScreenAdHistory(preferences[FIRST_OPEN_AT_KEY], preferences[LAST_SHOWN_AT_KEY])
    } catch (e: IOException) {
        null
    }

    override suspend fun recordFirstOpenIfAbsent(nowMillis: Long) {
        dataStore.edit { preferences ->
            if (preferences[FIRST_OPEN_AT_KEY] == null) preferences[FIRST_OPEN_AT_KEY] = nowMillis
        }
    }

    override suspend fun recordShown(nowMillis: Long) {
        dataStore.edit { preferences -> preferences[LAST_SHOWN_AT_KEY] = nowMillis }
    }

    private companion object {
        val FIRST_OPEN_AT_KEY = longPreferencesKey("ads_first_open_at_millis")
        val LAST_SHOWN_AT_KEY = longPreferencesKey("ads_last_full_screen_shown_at_millis")
    }
}
