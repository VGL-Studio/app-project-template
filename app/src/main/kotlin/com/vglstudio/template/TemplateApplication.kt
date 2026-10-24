package com.vglstudio.template

import android.app.Application
import com.vglstudio.template.core.ads.AdsController
import com.vglstudio.template.core.ads.AppActivityFilter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TemplateApplication : Application() {
    @Inject
    lateinit var ads: AdsController

    override fun onCreate() {
        super.onCreate()
        // :core:ads starts once, before any screen (HD-01). Only activities in the package of MainActivity count as the
        // app's; the SDK's own activities are ignored.
        ads.start(this, AppActivityFilter.inPackageOf(MainActivity::class.java))
    }
}
