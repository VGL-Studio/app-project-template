plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.template.core.config"
}

dependencies {
    implementation(project(":core:logging"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
}
