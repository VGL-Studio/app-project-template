package com.vglstudio.template

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ad gallery exists only in the staging build (plan T-014, KB-8): production has no class, string or manifest
 * entry of it. Production is not built on CI, so this scans the sources of :app. The working directory of a unit test
 * is the module directory.
 */
class AdGallerySourceSetTest {
    private val sources = File("src")

    // Staging holds the gallery; test sources mention it to check it.
    private val excludedSourceSets = setOf("staging", "testStaging", "test")

    private fun File.filesMentioningGallery(): List<File> = walkTopDown()
        .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
        .filter { file -> file.readText().let { it.contains("AdGallery") || it.contains("ad_gallery") } }
        .toList()

    @Test
    fun onlyStagingSourceSets_mentionAdGallery() {
        // Control: the source sets are where this test expects them, so an empty result means something.
        assertTrue("No src/main in ${sources.absolutePath}", File(sources, "main").isDirectory)
        val offenders = sources.listFiles().orEmpty()
            .filter { it.isDirectory && it.name !in excludedSourceSets }
            .flatMap { it.filesMentioningGallery() }
        assertTrue("Only src/staging may hold the ad gallery, found: $offenders", offenders.isEmpty())
    }

    @Test
    fun stagingDeclaresGallery() {
        val manifest = File(sources, "staging/AndroidManifest.xml")
        assertTrue("Missing ${manifest.path}", manifest.isFile)
        assertTrue(manifest.readText().contains("AdGalleryActivity"))
        val activity = File(sources, "staging/kotlin/com/vglstudio/template/adgallery/AdGalleryActivity.kt")
        assertTrue("Missing ${activity.path}", activity.isFile)
    }

    @Test
    fun mainManifest_hasOneLauncher() {
        val manifest = File(sources, "main/AndroidManifest.xml").readText()
        assertEquals(1, Regex("android\\.intent\\.category\\.LAUNCHER").findAll(manifest).count())
    }
}
