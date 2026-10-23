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

/**
 * Wires every feature's destinations together. Features never navigate to each other directly.
 * [onScreenChanged] receives the current screen for the network dialog: `null` before the first destination and on
 * the loading screen (spec network, KB-1), also after the system recreates the activity and restores the back stack.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier, onScreenChanged: (String?) -> Unit = {}) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val screen = reportedScreenOf(entry?.destination?.route)
    val currentOnScreenChanged by rememberUpdatedState(onScreenChanged)
    LaunchedEffect(screen) { currentOnScreenChanged(screen) }
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
