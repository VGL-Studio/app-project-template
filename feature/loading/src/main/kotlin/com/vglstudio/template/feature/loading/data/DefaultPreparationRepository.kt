package com.vglstudio.template.feature.loading.data

import com.vglstudio.template.core.assets.RemoteAssets
import com.vglstudio.template.core.config.RemoteConfigRefresher
import javax.inject.Inject

internal class DefaultPreparationRepository @Inject constructor(
    private val refresher: RemoteConfigRefresher,
    private val assets: RemoteAssets,
) : PreparationRepository {
    override suspend fun run(task: PreparationTask): Boolean = TODO("T-001")
}
