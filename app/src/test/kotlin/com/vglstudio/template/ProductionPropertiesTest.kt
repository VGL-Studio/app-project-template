package com.vglstudio.template

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AppsFlyer dev key in app/build.gradle.kts (ADR-0027, decision 2; plan T-033): production with the AppsFlyer
 * SDK cannot be built without it (note of 2026-10-29 on ADR-0027: an app without the SDK needs no key), staging has
 * none and reads no parameter. Production is not built on CI (ADR-0007), so this reads the build file, which the
 * test task declares as an input. The working directory of a unit test is the module directory.
 */
class ProductionPropertiesTest {
    private val build: String by lazy {
        val file = File("build.gradle.kts")
        assertTrue("Missing ${file.absolutePath}", file.isFile)
        file.readText()
    }

    /** The text of the flavor block [name], up to the next flavor or the end of `productFlavors`. */
    private fun flavor(name: String): String {
        val start = build.indexOf("named(\"$name\")")
        assertTrue("No flavor $name", start >= 0)
        val next = listOf(build.indexOf("named(", start + 1), build.indexOf("missingProductionProperties", start))
            .filter { it > start }
            .minOrNull() ?: build.length
        return build.substring(start, next)
    }

    @Test
    fun appsFlyerDevKey_isRequiredForProductionWithTheSdk() {
        // The check of the production pre-build task: the key is missing when the SDK is on the runtime classpath
        // and the parameter is absent or blank.
        val check = build
            .substringAfter("val missingProductionProperties = mapOf(")
            .substringBefore("throw GradleException(")
        assertTrue(check, "?.group == \"com.appsflyer\"" in check)
        assertTrue(
            check,
            "if (hasAppsFlyer && providers.gradleProperty(\"appsFlyerDevKey\").orNull.isNullOrBlank())" in check,
        )
        assertTrue(check, "mapOf(\"appsFlyerDevKey\" to \"ADR-0027\")" in check)
        assertTrue(check, "if (missing.isNotEmpty())" in check)
        val production = flavor("production")
        assertTrue(production, "\"APPSFLYER_DEV_KEY\"" in production)
        assertTrue(production, "gradleProperty(\"appsFlyerDevKey\")" in production)
    }

    @Test
    fun stagingKey_isFixedEmpty() {
        val staging = flavor("staging")
        // Control: the block read is the staging flavor.
        assertTrue(staging, "ASSETS_BASE_URL" in staging)
        assertTrue(staging, """buildConfigField("String", "APPSFLYER_DEV_KEY", "\"\"")""" in staging)
        assertFalse(staging, "appsFlyerDevKey" in staging)
    }
}
