plugins {
    alias(libs.plugins.template.android.application)
    alias(libs.plugins.template.android.compose)
    alias(libs.plugins.template.hilt)
    // Firebase config: app/src/staging/google-services.json is a fake for builds without secrets;
    // a real file in src/staging/debug, src/staging/release or src/production takes precedence (ADR-0012).
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.vglstudio.template"

    defaultConfig {
        // Replaced by the app's real id when a new app is created from the template.
        applicationId = "com.vglstudio.template"
        versionCode = 1
        versionName = "0.1.0"
    }
}

// The google-services task picks its config file when it runs, and the build cache can return the output made
// from another file (seen: the fake config came back from cache after the real file was added). Never cache it.
tasks.matching { it.name.endsWith("GoogleServices") }.configureEach {
    outputs.doNotCacheIf("google-services.json is chosen at execution time") { true }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:config"))
    implementation(project(":core:analytics"))
    implementation(project(":core:ads"))

    implementation(project(":feature:sample"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
