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
