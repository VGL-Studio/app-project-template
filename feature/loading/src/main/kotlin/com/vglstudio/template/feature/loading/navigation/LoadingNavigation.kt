package com.vglstudio.template.feature.loading.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.template.feature.loading.LoadingRoute
import kotlinx.serialization.Serializable

/** Navigation destination of the loading feature. It is the start destination of the app. */
@Serializable
data object LoadingDestination

/** [onFinished] is called once, when everything is ready (or the feature is off). */
fun NavGraphBuilder.loadingScreen(onFinished: () -> Unit) {
    composable<LoadingDestination> {
        LoadingRoute(onFinished = onFinished)
    }
}
