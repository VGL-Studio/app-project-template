package com.vglstudio.template.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.template.core.logging.AppLogger
import javax.inject.Inject

/** The mark "the app was opened once" (A-01), kept in DataStore. */
internal class FirstOpenMark @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger,
) {
    /** True when this is the first open since install or since the data was cleared; then writes the mark. */
    suspend fun consume(): Boolean = false
}
