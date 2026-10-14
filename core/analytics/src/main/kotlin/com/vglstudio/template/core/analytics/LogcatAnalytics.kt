package com.vglstudio.template.core.analytics

import android.util.Log
import javax.inject.Inject

/** Writes events to Logcat. Replaced by Firebase Analytics when Firebase is wired in. */
class LogcatAnalytics @Inject constructor() : Analytics {
    override fun log(event: AnalyticsEvent) {
        Log.d(TAG, "${event.name} ${event.params}")
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
