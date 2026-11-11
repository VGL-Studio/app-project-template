package com.vglstudio.template.network

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.template.core.designsystem.component.NetworkDialogTestTags
import com.vglstudio.template.core.network.NetworkDialogState
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Text of the network dialog in each language of the template: `res/values/` of `:app` is English,
 * `res/values-vi/` is Vietnamese. `fr` stands for any language without its own folder, so it proves `values/` itself
 * is English. The dialog is built as in NetworkDialogHostTest.
 *
 * Runs only in the template repository (it has `scripts/new-app.sh`, which `new-app.sh` deletes in an app): in an app
 * the whole class is reported as skipped, since the dialog text belongs to the app, which may change it or keep a
 * single language.
 *
 * The test reads `scripts/new-app.sh` outside the inputs Gradle knows, so Gradle may consider the test task up to date
 * when only that file changes: run it with `--rerun` when checking.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NetworkDialogLanguageTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun onlyInTemplate() {
        assumeTrue("Not the template repository", File(findRepoRoot(), "scripts/new-app.sh").isFile)
    }

    @Test
    @Config(qualifiers = "vi")
    fun shown_vietnamese_vi() {
        setContentButton(STILL_OFFLINE)

        composeRule.onAllNodesWithText("Không có kết nối mạng").assertCountEquals(1)
        composeRule.onAllNodesWithText(
            "Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại.",
        ).assertCountEquals(1)
        composeRule.onAllNodesWithText("Vẫn chưa có kết nối. Hãy thử lại sau ít phút.").assertCountEquals(1)
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).assertTextEquals("Thử lại")
    }

    @Test
    @Config(qualifiers = "en")
    fun shown_english_en() = assertEnglish()

    @Test
    @Config(qualifiers = "fr")
    fun shown_english_otherLocale() = assertEnglish()

    private fun assertEnglish() {
        setContentButton(STILL_OFFLINE)

        composeRule.onAllNodesWithText("No internet connection").assertCountEquals(1)
        composeRule.onAllNodesWithText(
            "This app needs the Internet to work. Check Wi-Fi or mobile data, then try again.",
        ).assertCountEquals(1)
        composeRule.onAllNodesWithText("Still no connection. Please try again in a moment.").assertCountEquals(1)
        composeRule.onNodeWithTag(NetworkDialogTestTags.RETRY).assertTextEquals("Try again")
    }

    private fun setContentButton(state: NetworkDialogState) {
        composeRule.setContent {
            NetworkDialogHost(state = state, onRetry = {}, onBack = {}) {
                Button(onClick = {}) { Text("Content") }
            }
        }
        composeRule.waitForIdle()
    }

    private fun findRepoRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").isFile) dir = dir.parentFile
        return checkNotNull(dir) { "No settings.gradle.kts above ${File("").absolutePath}" }
    }

    private companion object {
        val STILL_OFFLINE = NetworkDialogState.Shown(checking = false, stillOffline = true)
    }
}
