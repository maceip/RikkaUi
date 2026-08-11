plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.gradle.ktlint)
}

android {
    namespace = "dev.rikkaui.components"
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
    api(platform(libs.androidx.compose.bom))
    api(projects.foundation)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)

    // RikkaUI owns the semantic icon contract. Consumers choose the supported
    // Phosphor weight at their composition root.
    api(libs.rikka.icons.core)
    api(libs.rikka.icons.tokens.core)
    api(libs.rikka.icons.pack.phosphor)

    // `api`: the glass components expose Backdrop/LayerBackdrop in their signatures.
    api(libs.backdrop)
    // `implementation`: wrapped behind SwipeableRow and TranscriptText, so
    // neither reaches a public signature.
    implementation(libs.swipe)
    implementation(libs.extendedspans)
}

ktlint {
    ignoreFailures = true
}
