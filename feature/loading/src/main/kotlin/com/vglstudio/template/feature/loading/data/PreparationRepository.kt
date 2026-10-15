package com.vglstudio.template.feature.loading.data

interface PreparationRepository {
    /** Runs one task. Returns true when it is done, false when it could not be done (retrying may help). */
    suspend fun run(task: PreparationTask): Boolean
}
