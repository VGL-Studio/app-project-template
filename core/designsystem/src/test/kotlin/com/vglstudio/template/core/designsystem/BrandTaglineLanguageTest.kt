package com.vglstudio.template.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The brand tagline of the template in each language: `res/values/` is English, `res/values-vi/` is Vietnamese.
 * `fr` stands for any language without its own folder, so it proves `values/` itself is English.
 *
 * Runs only in the template repository (it has `scripts/new-app.sh`, which `new-app.sh` deletes in an app): in an app
 * the whole class is reported as skipped, since each app replaces the tagline with its own (ADR-0008).
 *
 * The test reads `scripts/new-app.sh` outside the inputs Gradle knows, so Gradle may consider the test task up to date
 * when only that file changes: run it with `--rerun` when checking.
 */
@RunWith(AndroidJUnit4::class)
class BrandTaglineLanguageTest {
    @Before
    fun onlyInTemplate() {
        assumeTrue("Not the template repository", File(findRepoRoot(), "scripts/new-app.sh").isFile)
    }

    @Test
    @Config(qualifiers = "fr")
    fun otherLocale_isEnglish() {
        assertEquals("Everything in rhythm.", tagline())
    }

    @Test
    @Config(qualifiers = "vi")
    fun vietnamese_isVietnamese() {
        assertEquals("Mọi việc vào đúng nhịp.", tagline())
    }

    private fun tagline(): String =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.brand_tagline)

    private fun findRepoRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").isFile) dir = dir.parentFile
        return checkNotNull(dir) { "No settings.gradle.kts above ${File("").absolutePath}" }
    }
}
