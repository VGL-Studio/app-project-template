plugins {
    alias(libs.plugins.template.android.feature)
}

android {
    namespace = "com.vglstudio.template.feature.sample"
}

dependencies {
    implementation(project(":core:data"))
}
