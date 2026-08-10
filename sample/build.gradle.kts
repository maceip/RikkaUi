plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "dev.rikkaui.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.rikkaui.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        // Compose previews: `compose = true` turns on the compiler for this
        // module, and ui-tooling-preview below supplies @Preview itself.
        compose = true
    }

    compileOptions {
        // Must match :components / :foundation, whose Kotlin output follows the
        // JDK 21 toolchain. A lower toolchain here cannot load their class files
        // in the Paparazzi test JVM.
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.components)
    implementation(projects.foundation)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.androidx.activity.compose)

    // Preview annotation + the renderer Android Studio uses for the preview pane.
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.junit)
}
