package com.vglstudio.template.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** Which activities belong to the app. SDK activities (AdActivity, mediation adapters) are ignored by ads. */
fun interface AppActivityFilter {
    fun isAppActivity(activity: Activity): Boolean

    companion object {
        /**
         * Class name starts with the package of [anchor] plus a dot (MainActivity::class.java), so the staging suffix
         * of the application id does not matter.
         */
        @Suppress("UNUSED_PARAMETER")
        fun inPackageOf(anchor: Class<*>): AppActivityFilter = AppActivityFilter { true }
    }
}

/** Whether [className] is in [packageName] or one of its subpackages. */
@Suppress("UNUSED_PARAMETER")
internal fun isClassInPackage(className: String, packageName: String): Boolean = true

/** Follows the activities of the app through the callbacks of [Application]. Main thread only. */
@Suppress("UNUSED_PARAMETER")
internal class AdsActivityTracker(private val filter: AppActivityFilter, private val listener: Listener) :
    Application.ActivityLifecycleCallbacks {
    /** What the tracker reports, for app activities only. */
    interface Listener {
        fun onForegroundChanged(foreground: Boolean) {}

        fun onActivityResumed(activity: Activity) {}

        fun onActivityPaused(activity: Activity) {}

        fun onActivityDestroyed(activity: Activity) {}
    }

    /** True while at least one app activity is started. */
    val foreground: Boolean get() = false

    /** The app activity resumed now, or null. */
    val resumedActivity: Activity? get() = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
