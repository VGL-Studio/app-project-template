package com.vglstudio.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.vglstudio.template.core.ads.AdsController
import com.vglstudio.template.core.ads.FullScreenAdHost
import com.vglstudio.template.core.ads.LocalAdsController
import com.vglstudio.template.core.designsystem.theme.AppTheme
import com.vglstudio.template.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var ads: AdsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Shows the consent form when required; the ads SDK starts only after it (ADR-0004).
        ads.gatherConsent(this)
        setContent {
            CompositionLocalProvider(LocalAdsController provides ads) {
                AppTheme {
                    // The surface fills the whole window, including the area behind the system bars,
                    // so those areas take the theme background in both light and dark mode.
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost()
                    }
                    // Native full-screen ads open as a dialog over the current screen (ADR-0022).
                    FullScreenAdHost()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ads.onAppForeground()
    }

    override fun onStop() {
        super.onStop()
        ads.onAppBackground()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        ads.onUserInteraction()
    }
}
