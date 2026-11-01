package com.vglstudio.template

import java.io.File
import org.junit.Assert.assertEquals
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

    @Test
    fun testDeviceIds_readOnlyByStagingFlavor() {
        // Plan T-058, KB-28: the developer's test devices come from local.properties, read by the staging flavor only.
        val staging = flavor("staging")
        // Control: the block read is the staging flavor.
        assertTrue(staging, "ASSETS_BASE_URL" in staging)
        assertTrue(staging, "localProperties.getProperty(\"ads.testDeviceIds\")" in staging)
        assertTrue(staging, "buildConfigField(\"String\", \"ADS_TEST_DEVICE_IDS\"" in staging)
        // The shared localProperties at the top of the file names no key: every read of the key is in staging.
        assertEquals(staging.occurrences(TEST_DEVICES_KEY), build.occurrences(TEST_DEVICES_KEY))
    }

    @Test
    fun production_hasNoTestDeviceField() {
        val production = flavor("production")
        // Control: the block read is the production flavor.
        assertTrue(production, "AD_UNITS" in production)
        // Control (L-055): the names looked for are those of the staging flavor.
        val staging = flavor("staging")
        FORBIDDEN_IN_PRODUCTION.forEach { assertTrue("$it not in staging", it in staging) }

        FORBIDDEN_IN_PRODUCTION.forEach { assertFalse("$it in production:\n$production", it in production) }
    }

    @Test
    fun maxSdkKey_isOptionalUntilReleaseShPassesIt() {
        // Plan T-068, question 6: production reads -PmaxSdkKey but does not require it yet. T-073 changes this test
        // together with the export in scripts/release.sh and the L-125 contract test of scripts/tests/run.sh (the
        // "8 none" and "4 none" lines), so no release breaks after the passwords are typed.
        val production = flavor("production")
        // Control: the block read is the production flavor.
        assertTrue(production, "appsFlyerDevKey" in production)
        assertTrue(production, "\"MAX_SDK_KEY\"" in production)
        assertTrue(production, "gradleProperty(\"maxSdkKey\")" in production)
        val check = build
            .substringAfter("val missingProductionProperties = mapOf(")
            .substringBefore("throw GradleException(")
        listOf("maxSdkKey", "com.applovin", "ADR-0037").forEach { assertFalse("$it in:\n$check", it in check) }
    }

    @Test
    fun stagingMaxKey_readsLocalPropertiesOnly() {
        // Plan T-068, question 43 of the ads spec: CI has no such key, so its staging builds never start MAX.
        val staging = flavor("staging")
        assertTrue(staging, "\"MAX_SDK_KEY\"" in staging)
        assertTrue(staging, "local.properties" in staging)
        assertTrue(staging, "\"$MAX_KEY\"" in staging)
        assertFalse(staging, "gradleProperty(\"maxSdkKey\")" in staging)
        // Control: production never reads the developer's key.
        assertFalse(flavor("production"), MAX_KEY in flavor("production"))
    }

    @Test
    fun stagingMaxKey_isFilteredBeforeBuildConfig() {
        // A quote or backslash in local.properties would break the Java string of BuildConfig (plan T-068).
        val staging = flavor("staging")
        assertTrue(staging, "getProperty(\"$MAX_KEY\")" in staging)
        assertTrue(staging, ".trim()" in staging)
        assertTrue(staging, "Regex(\"[A-Za-z0-9_-]+\")" in staging)
        assertTrue(staging, "matches" in staging)
        val field = staging.lines().single { "buildConfigField(\"String\", \"MAX_SDK_KEY\"" in it }
        assertTrue(field, "\$maxSdkKey" in field)
        assertFalse(field, "getProperty(" in field)
        val warning = staging.lines().single { "logger.warn(" in it }
        assertTrue(warning, MAX_KEY in warning)
        assertFalse("The warning prints a value: $warning", "$" in warning)
        // Control (L-054): local.properties is read once, in the one variable shared with T-058.
        assertEquals(1, build.occurrences("val localProperties"))
        assertEquals(1, build.occurrences("import java.util.Properties"))
    }

    @Test
    fun maxAdUnits_stagingReadsLocalPropertiesFiltered_productionOptional() {
        // Plan T-071, step 7: the MAX ad units of the slots are never in the repo; staging reads them from
        // local.properties only (filtered for BuildConfig), production from -PmaxAdUnits, optional until T-073.
        val staging = flavor("staging")
        assertTrue(staging, "getProperty(\"ads.maxAdUnits\")" in staging)
        assertTrue(staging, ".trim()" in staging)
        assertTrue(staging, "Regex(\"[A-Za-z0-9_=;-]*\")" in staging)
        assertTrue(staging, "\"MAX_AD_UNITS\"" in staging)
        assertFalse(staging, "gradleProperty(\"maxAdUnits\")" in staging)
        val production = flavor("production")
        assertTrue(production, "\"MAX_AD_UNITS\"" in production)
        assertTrue(production, "gradleProperty(\"maxAdUnits\")" in production)
        assertFalse(production, "ads.maxAdUnits" in production)
        val check = build
            .substringAfter("val missingProductionProperties = mapOf(")
            .substringBefore("throw GradleException(")
        // Control: the check read is the one that names the AppsFlyer key.
        assertTrue(check, "appsFlyerDevKey" in check)
        assertFalse(check, "maxAdUnits" in check)
    }

    private fun String.occurrences(part: String): Int = split(part).size - 1

    private companion object {
        const val TEST_DEVICES_KEY = "ads.testDeviceIds"
        const val MAX_KEY = "ads.maxSdkKey"
        val FORBIDDEN_IN_PRODUCTION = listOf("ADS_TEST_DEVICE_IDS", "testDeviceIds", "localProperties")
    }
}
