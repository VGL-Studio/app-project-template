// Root build. Plugins are declared here with `apply false` so that build-logic
// (which only has them as compileOnly) shares the same classloader.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.ktlint) apply false
}

/**
 * Module dependency rule (ADR-0001): a feature module may depend on core modules only,
 * never on another feature module. Run by scripts/check.sh and CI.
 */
tasks.register("checkModuleDependencies") {
    group = "verification"
    description = "Fails if any :feature:* module depends on another :feature:* module."
    val violations = provider {
        subprojects
            .filter { it.path.startsWith(":feature:") }
            .flatMap { feature ->
                feature.configurations
                    .flatMap { configuration ->
                        configuration.dependencies
                            .withType(ProjectDependency::class.java)
                            .map { it.path }
                    }
                    .filter { it.startsWith(":feature:") && it != feature.path }
                    .distinct()
                    .map { "${feature.path} -> $it" }
            }
    }
    doLast {
        val found = violations.get()
        if (found.isNotEmpty()) {
            throw GradleException(
                "Feature modules must not depend on other feature modules (ADR-0001):\n" +
                    found.joinToString("\n") { "  $it" },
            )
        }
        logger.lifecycle("Module dependency rule OK.")
    }
}
