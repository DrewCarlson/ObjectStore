@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.mavenPublish)
}

androidLib("objectstore.secure")

kotlin {
    androidTarget { publishAllLibraryVariants() }
    jvm()
    jsAll(enableBrowser = false)
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
                implementation(libs.okio)
                implementation(libs.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.okio.fakefilesystem)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.okio.nodefilesystem)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val nativeCommonMain by creating { dependsOn(commonMain) }
        val linuxCommonMain by getting { dependsOn(nativeCommonMain) }
        val mingwX64Main by getting { dependsOn(nativeCommonMain) }

        val darwinCommonMain by creating { dependsOn(nativeCommonMain) }
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
