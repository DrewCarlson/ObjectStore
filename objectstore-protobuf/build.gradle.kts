@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    alias(libs.plugins.mavenPublish)
}

androidLib("objectstore.protobuf")

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
                api(libs.serialization.protobuf)
            }
        }
    }
}
