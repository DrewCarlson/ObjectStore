@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    alias(libs.plugins.mavenPublish)
}

apply(plugin = "kotlinx-atomicfu")

androidLib("objectstore.core")

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
                api(libs.coroutines.core)
            }
        }

        val nonJsMain by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":objectstore-cbor"))
                implementation(project(":objectstore-json"))
                implementation(project(":objectstore-protobuf"))
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(libs.robolectric)
                implementation(project(":objectstore-secure"))
            }
        }

        val jvmTest by getting {
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

        val jvmMain by getting { dependsOn(nonJsMain) }
        val androidMain by getting { dependsOn(nonJsMain) }
        val linuxCommonMain by getting { dependsOn(nonJsMain) }
        val mingwX64Main by getting { dependsOn(nonJsMain) }

        val darwinCommonMain by creating {
            dependsOn(commonMain)
            dependsOn(nonJsMain)
        }
        val iosCommonMain by getting { dependsOn(darwinCommonMain) }
        val macosCommonMain by getting { dependsOn(darwinCommonMain) }
        val tvosCommonMain by getting { dependsOn(darwinCommonMain) }
        val watchosCommonMain by getting { dependsOn(darwinCommonMain) }

        val darwinCommonTest by creating { dependsOn(commonTest) }
        val iosCommonTest by getting { dependsOn(darwinCommonTest) }
        val macosCommonTest by getting { dependsOn(darwinCommonTest) }
        val tvosCommonTest by getting { dependsOn(darwinCommonTest) }
        val watchosCommonTest by getting { dependsOn(darwinCommonTest) }
    }
}
