buildscript {
    // Paparazzi 2.0.0-alpha05 needs a layoutlib new enough for Compose UI 1.9
    // (which calls the API 35 StaticLayout.Builder.setUseBoundsForWidth), but it
    // drags com.android.tools 31.13.2 onto the shared buildscript classpath.
    // AGP 8.10's own VersionCheckPlugin then dies on the GradleVersion class that
    // 31.13 removed. Pinning the tools stack to AGP 8.10's aligned version keeps
    // AGP working while still giving Paparazzi its newer layoutlib artifact,
    // which is versioned separately from the tools libraries.
    configurations.classpath {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.android.tools" && requested.name != "layoutlib") {
                useVersion("31.10.0")
            }
        }
    }
}

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.gradle.ktlint)
    alias(libs.plugins.vanniktechMavenPublish) apply false
}

subprojects {
    afterEvaluate {
        tasks.configureEach {
            when (name) {
                "preBuild",
                "wasmJsBrowserDevelopmentExecutableDistribution",
                -> {
                    val ktlintFormat = tasks.findByName("ktlintFormat")
                    if (ktlintFormat != null) dependsOn(ktlintFormat)
                }
            }
        }
    }
}
