package com.vglstudio.template.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vglstudio.template.feature.loading.navigation.LoadingDestination
import com.vglstudio.template.feature.loading.navigation.loadingScreen
import com.vglstudio.template.feature.sample.navigation.SampleDestination
import com.vglstudio.template.feature.sample.navigation.sampleScreen
import com.vglstudio.template.network.reportedScreenOf
import com.vglstudio.template.network.reportsScreen

/**
 * Wires every feature's destinations together. Features never navigate to each other directly.
 * [onScreenChanged] receives the current screen for the network dialog: `null` on the loading screen (spec network,
 * KB-1), also after the system recreates the activity and restores the back stack. Nothing is reported while the
 * back stack has no entry yet, so a recreation never looks like a passage through loading ([reportsScreen]).
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier, onScreenChanged: (String?) -> Unit = {}) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentOnScreenChanged by rememberUpdatedState(onScreenChanged)
    LaunchedEffect(route) {
        if (reportsScreen(route)) currentOnScreenChanged(reportedScreenOf(route))
    }
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
