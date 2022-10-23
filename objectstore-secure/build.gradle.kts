plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

apply(from = "../gradle/publishing.gradle.kts")

androidLib("objectstore.secure")

kotlin {
    android { publishAllLibraryVariants() }
    iosAll()
    macosAll()
    tvosAll()
    watchosAll()

    sourceSets {
        all { explicitApi() }

        val commonMain by getting {
            dependencies {
                api(project(":objectstore-core"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.security.crypto)
            }
        }

        val darwinCommonMain by creating { dependsOn(commonMain) }
        val iosCommonMain by getting { dependsOn(darwinCommonMain) }
        val macosCommonMain by getting { dependsOn(darwinCommonMain) }
        val tvosCommonMain by getting { dependsOn(darwinCommonMain) }
        val watchosCommonMain by getting { dependsOn(darwinCommonMain) }
    }
}
