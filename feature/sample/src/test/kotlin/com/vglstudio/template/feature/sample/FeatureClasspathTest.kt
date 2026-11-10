package com.vglstudio.template.feature.sample

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Keeps the `implementation` declaration of activity-compose in the feature convention plugin:
 * test sources see `implementation` dependencies, so this file stops compiling when the line is removed.
 */
@RunWith(AndroidJUnit4::class)
class FeatureClasspathTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localActivity_resolvesHostActivity() {
        var activity: Activity? = null
        composeRule.setContent { activity = LocalActivity.current }
        composeRule.waitForIdle()
        assertNotNull(activity)
    }
}
