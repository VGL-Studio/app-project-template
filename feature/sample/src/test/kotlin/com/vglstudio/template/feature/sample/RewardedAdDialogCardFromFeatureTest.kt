package com.vglstudio.template.feature.sample

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.ads.RewardedAdDialogCard
import com.vglstudio.template.core.ads.RewardedAdDialogTestTags
import com.vglstudio.template.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A feature can draw the card of the rewarded dialog for its screenshot tests and previews, where no rewarded action
 * exists. The card is called from this feature module, so this class only compiles while the card is public.
 */
@RunWith(AndroidJUnit4::class)
class RewardedAdDialogCardFromFeatureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var watch = 0
    private var noThanks = 0

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    private fun setCard() {
        composeRule.setContent {
            AppTheme {
                RewardedAdDialogCard(
                    title = "Unlock export",
                    onWatch = { watch++ },
                    onNoThanks = { noThanks++ },
                ) {
                    Text("Feature content")
                }
            }
        }
    }

    @Test
    fun featureBuildsCard_showsTitleContentAndBothButtons() {
        setCard()

        composeRule.onNodeWithText("Unlock export").assertIsDisplayed()
        composeRule.onNodeWithText("Feature content").assertIsDisplayed()
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.WATCH)
            .assertTextEquals(string(com.vglstudio.template.core.ads.R.string.ads_reward_watch))
            .assertIsDisplayed()
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.NO_THANKS)
            .assertTextEquals(string(com.vglstudio.template.core.ads.R.string.ads_reward_no_thanks))
            .assertIsDisplayed()
    }

    @Test
    fun featureBuildsCard_buttonsCallTheirCallbacks() {
        setCard()

        composeRule.onNodeWithTag(RewardedAdDialogTestTags.WATCH).performClick()
        composeRule.runOnIdle {
            assertEquals(1, watch)
            assertEquals(0, noThanks)
        }
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.NO_THANKS).performClick()
        composeRule.runOnIdle {
            assertEquals(1, noThanks)
            assertEquals(1, watch)
        }
    }
}
