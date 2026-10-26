package com.vglstudio.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vglstudio.template.core.ads.AdsController
import com.vglstudio.template.core.ads.AdsHost
import com.vglstudio.template.core.ads.FullScreenAdHost
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.core.network.NetworkDialogController
import com.vglstudio.template.navigation.AppNavHost
import com.vglstudio.template.network.NetworkDialogHost
import com.vglstudio.template.ui.APP_ROOT_TAG
import com.vglstudio.template.ui.exposeTestTagsForUiAutomator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var ads: AdsController

    @Inject
    lateinit var networkDialog: NetworkDialogController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Shows the consent form when required; the ads SDK starts only after it (ADR-0004).
        ads.gatherConsent(this)
        setContent {
            // Provides the ads controller and reports the block of resume ads (G-04).
            AdsHost(ads) {
                // The network dialog waits while a full-screen ad is on screen (spec network, KB-2).
                val fullScreenAdShowing by ads.fullScreenAdShowing.collectAsState()
                LaunchedEffect(fullScreenAdShowing) { networkDialog.setFullScreenAdShowing(fullScreenAdShowing) }
                AppTheme {
                    // The surface fills the whole window, including the area behind the system bars,
                    // so those areas take the theme background in both light and dark mode.
                    // testTag shows as resource-id in uiautomator dump, for the scenario files of the plans.
                    Surface(modifier = Modifier.fillMaxSize().exposeTestTagsForUiAutomator().testTag(APP_ROOT_TAG)) {
                        val networkDialogState by networkDialog.state.collectAsState()
                        // Back while the network dialog shows sends the app to the background (plan T-017).
                        NetworkDialogHost(
                            state = networkDialogState,
                            onRetry = networkDialog::retry,
                            onBack = { moveTaskToBack(true) },
                        ) {
                            AppNavHost(onScreenChanged = networkDialog::setScreen)
                        }
                    }
                    // Native full-screen ads open as a dialog over the current screen (ADR-0022).
                    FullScreenAdHost()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Back from the background without network: the dialog shows at once (spec network, KB-2).
        networkDialog.onAppForeground()
    }
}
