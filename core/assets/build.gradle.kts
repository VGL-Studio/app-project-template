plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.template.core.assets"
}

dependencies {
    implementation(project(":core:logging"))
}
