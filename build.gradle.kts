import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
    alias(libs.plugins.binaryCompat)
    alias(libs.plugins.completeKotlin)
}

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    dependencies {
        classpath(libs.agp)
        classpath(libs.atomicfu.plugin) {
            exclude("org.jetbrains.kotlin", "kotlin-gradle-plugin-api")
        }
    }
}

allprojects {
    plugins.withType<NodeJsRootPlugin> {
        the<YarnRootExtension>().lockFileDirectory = rootDir.resolve("gradle/kotlin-js-store")
    }

    repositories {
        mavenCentral()
        google()
    }
}

extensions.configure<kotlinx.kover.api.KoverMergedConfig> {
    enable()
    filters {
        classes {
            excludes += "objectstore.*.BuildConfig"
        }
    }
}

System.getenv("GITHUB_REF")?.let { ref ->
    if (ref.startsWith("refs/tags/v")) {
        version = ref.substringAfterLast("refs/tags/v")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    kover {}

    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/**.kt")
            licenseHeaderFile(rootDir.resolve("licenseHeader.txt"))
            ktlint(libs.versions.ktlint.get())
                .editorConfigOverride(mapOf("disabled_rules" to "no-wildcard-imports,no-unused-imports"))
        }
    }

    afterEvaluate {
        listOfNotNull(
            tasks.findByName("iosSimulatorArm64Test"),
            tasks.findByName("iosX64Test")
        ).forEach {
            (it as KotlinNativeSimulatorTest)
            it.deviceId = "iPhone 14"
        }
    }
}