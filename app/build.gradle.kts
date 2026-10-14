plugins {
    alias(libs.plugins.template.android.application)
    alias(libs.plugins.template.android.compose)
    alias(libs.plugins.template.hilt)
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

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
