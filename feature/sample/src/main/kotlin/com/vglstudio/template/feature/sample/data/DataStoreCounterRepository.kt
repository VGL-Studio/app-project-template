package com.vglstudio.template.feature.sample.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

internal class DataStoreCounterRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    CounterRepository {
    override val count: Flow<Int> = dataStore.data
        // A read error must never crash the screen: fall back to empty data.
        // Corrupted files are handled once for the whole app in :core:data (DataStoreModule).
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[COUNT_KEY] ?: 0 }

    override suspend fun increment() {
        dataStore.edit { preferences ->
            preferences[COUNT_KEY] = (preferences[COUNT_KEY] ?: 0) + 1
        }
    }

    private companion object {
        val COUNT_KEY = intPreferencesKey("sample_count")
    }
}
