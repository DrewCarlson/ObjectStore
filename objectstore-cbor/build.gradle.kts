plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

apply(from = "../gradle/publishing.gradle.kts")

androidLib("objectstore.cbor")

kotlin {
    android { publishAllLibraryVariants() }
    jvm()
    jsAll()
    iosAll()
    tvosAll()
    watchosAll()
    macosAll()
    linuxAll()
    mingwX64()

    sourceSets {
        all { explicitApi() }

        val commonMain by getting {
            dependencies {
                api(project(":objectstore-core"))
                implementation(libs.serialization.core)
                api(libs.serialization.cbor)
            }
        }
    }
}
