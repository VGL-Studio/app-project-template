package com.vglstudio.template.core.ads

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** G-06: on every resume, the stuck flag is released before consent is gathered again. */
@RunWith(AndroidJUnit4::class)
class ActivityResumeStepsTest {
    @Test
    fun unstickRunsBeforeConsentRegather() {
        val steps = mutableListOf<String>()
        val resume = ActivityResumeSteps(
            recoverStuck = { steps += "recoverStuck" },
            regatherConsent = { steps += "regatherConsent" },
            releaseLeaveMark = { steps += "releaseLeaveMark" },
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).get()
        resume.onResumed(activity)
        assertEquals(listOf("recoverStuck", "regatherConsent", "releaseLeaveMark"), steps)
    }
}
