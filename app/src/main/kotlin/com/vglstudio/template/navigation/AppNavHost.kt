package com.vglstudio.template.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.vglstudio.template.feature.loading.navigation.LoadingDestination
import com.vglstudio.template.feature.loading.navigation.loadingScreen
import com.vglstudio.template.feature.sample.navigation.SampleDestination
import com.vglstudio.template.feature.sample.navigation.sampleScreen

/** Wires every feature's destinations together. Features never navigate to each other directly. */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LoadingDestination,
        modifier = modifier.safeDrawingPadding(),
    ) {
        loadingScreen(
            onFinished = {
                // Loading leaves the back stack: Back on the start screen closes the app (spec loading, KB-1).
                navController.navigate(SampleDestination) {
                    popUpTo<LoadingDestination> { inclusive = true }
                }
            },
        )
        sampleScreen()
    }
}
