package com.vglstudio.template.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.template.core.logging.AppLogger
import javax.inject.Inject

// Skeleton of plan T-026, step 3.
internal class InterDisplayCounter @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger,
) {
    suspend fun increment(): Int? = null
}
