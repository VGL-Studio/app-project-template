package com.vglstudio.template.feature.sample.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreCounterRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    CounterRepository {
    override val count: Flow<Int> = dataStore.data.map { preferences -> preferences[COUNT_KEY] ?: 0 }

    override suspend fun increment() {
        dataStore.edit { preferences ->
            preferences[COUNT_KEY] = (preferences[COUNT_KEY] ?: 0) + 1
        }
    }

    private companion object {
        val COUNT_KEY = intPreferencesKey("sample_count")
    }
}
