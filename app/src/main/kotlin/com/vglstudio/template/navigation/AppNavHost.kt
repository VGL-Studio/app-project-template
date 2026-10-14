package com.vglstudio.template.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.vglstudio.template.feature.sample.navigation.SampleDestination
import com.vglstudio.template.feature.sample.navigation.sampleScreen

/** Wires every feature's destinations together. Features never navigate to each other directly. */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SampleDestination,
        modifier = modifier.safeDrawingPadding(),
    ) {
        sampleScreen()
    }
}
