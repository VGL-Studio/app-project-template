package com.vglstudio.template.adgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.vglstudio.template.core.ads.AdsController
import com.vglstudio.template.core.ads.FullScreenAdHost
import com.vglstudio.template.core.ads.LocalAdsController
import com.vglstudio.template.core.designsystem.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The staging ad gallery (plan T-014), opened from its own launcher icon "Ad gallery"; declared only in the staging
 * manifest. It asks for consent like MainActivity, since testers may open it first. It does not report foreground and
 * background to the ads controller: ads sessions and per-session limits follow MainActivity only.
 */
@AndroidEntryPoint
class AdGalleryActivity : ComponentActivity() {
    @Inject
    lateinit var ads: AdsController

    private val viewModel: AdGalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Shows the consent form when required; the ads SDK starts only after it (ADR-0004).
        ads.gatherConsent(this)
        setContent {
            CompositionLocalProvider(LocalAdsController provides ads) {
                AppTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AdGalleryRoute(
                            viewModel = viewModel,
                            onExit = ::finish,
                            modifier = Modifier.safeDrawingPadding(),
                        )
                    }
                    // Native full-screen ads open as a dialog over the gallery, as in MainActivity (ADR-0022).
                    FullScreenAdHost()
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        ads.onUserInteraction()
    }
}
