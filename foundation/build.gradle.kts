plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.gradle.ktlint)
}

android {
    namespace = "dev.rikkaui.foundation"
    compileSdk = 35
    defaultConfig {
        // Liquid glass needs the AGSL refraction shader, which is API 33.
        // Below that the material cannot exist, so this is the floor.
        minSdk = 33
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    explicitApi()
    jvmToolchain(21)
}

dependencies {
    // `api`: Color, Dp, Modifier and TextStyle are all over the public token API.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
}

ktlint {
    ignoreFailures = true
}
