plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.template.core.network"
}

dependencies {
    implementation(project(":core:config"))
    implementation(project(":core:analytics"))
    implementation(project(":core:logging"))

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
