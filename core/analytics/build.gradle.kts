plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.template.core.analytics"
}

dependencies {
    implementation(project(":core:config"))
    implementation(project(":core:logging"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
