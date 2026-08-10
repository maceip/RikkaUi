import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.gradle.ktlint)
    alias(libs.plugins.vanniktechMavenPublish)
}

android {
    namespace = "dev.rikkaui.components"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    explicitApi()

    androidTarget {
        publishLibraryVariants("release")
    }

    jvm("desktop")

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    js {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(projects.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                // RikkaUI owns the semantic icon contract. Consumers choose
                // the supported Phosphor weight at their composition root.
                api(libs.rikka.icons.core)
                api(libs.rikka.icons.tokens.core)
                api(libs.rikka.icons.pack.phosphor)
            }
        }

        androidMain {
            dependencies {
                // `api`, not `implementation`: the glass components expose
                // Backdrop/LayerBackdrop in their public signatures.
                api(libs.backdrop)
                // `implementation`: both are wrapped behind RikkaUI types
                // (GlassSwipeAction, TranscriptText) so neither leaks into a
                // public signature. Copy-paste consumers get them listed as
                // registry dependencies instead.
                implementation(libs.swipe)
                implementation(libs.extendedspans)
            }
        }
    }
}

ktlint {
    ignoreFailures = true
}

signing {
    useInMemoryPgpKeys(
        findProperty("signingInMemoryKeyId") as String?,
        findProperty("signingInMemoryKey") as String?,
        findProperty("signingInMemoryKeyPassword") as String?,
    )
}

mavenPublishing {
    pom {
        name = "RikkaUI Components"
        description = "40+ styled UI components for Compose Multiplatform — foundation-only, no Material3"
    }
}
