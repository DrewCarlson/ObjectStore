@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.mavenPublish)
}

androidLib("objectstore.secure") {
    defaultConfig {
        minSdk = 23
    }
}

kotlin {
    androidTarget { publishAllLibraryVariants() }
    iosAll()
    macosAll()
    tvosAll()
    watchosAll()

    sourceSets {
        all {
            explicitApi()
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        val commonMain by getting {
            dependencies {
                api(project(":objectstore-core"))
            }
        }
        val commonTest by getting

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.security.crypto)
            }
        }

        val darwinCommonMain by creating { dependsOn(commonMain) }
        val darwinCommonTest by creating { dependsOn(commonTest) }
        val iosCommonMain by getting { dependsOn(darwinCommonMain) }
        val iosCommonTest by getting { dependsOn(darwinCommonTest) }
        val macosCommonMain by getting { dependsOn(darwinCommonMain) }
        val macosCommonTest by getting { dependsOn(darwinCommonTest) }
        val tvosCommonMain by getting { dependsOn(darwinCommonMain) }
        val tvosCommonTest by getting { dependsOn(darwinCommonTest) }
        val watchosCommonMain by getting { dependsOn(darwinCommonMain) }
        val watchosCommonTest by getting { dependsOn(darwinCommonTest) }
    }
}
