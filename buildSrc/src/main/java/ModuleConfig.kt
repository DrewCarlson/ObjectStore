import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import com.android.build.gradle.LibraryExtension
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

fun KotlinMultiplatformExtension.jsAll(enableBrowser: Boolean = true) {
    js(IR) {
        nodejs()
        if (enableBrowser) {
            browser {
                testTask {
                    useKarma {
                        useFirefoxHeadless()
                    }
                }
            }
        }
    }
}

fun KotlinMultiplatformExtension.iosAll(enableArm32: Boolean = true) {
    iosAll(enableArm32) { }
}

fun KotlinMultiplatformExtension.iosAll(
    enableArm32: Boolean = true,
    configure: Action<KotlinNativeTarget>
) {
    if (enableArm32) {
        iosArm32(configure)
    }
    iosArm64(configure)
    iosX64 { configure.execute(this) }
    iosSimulatorArm64 { configure.execute(this) }

    val iosCommonMain = sourceSets.create("iosCommonMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    val iosCommonTest = sourceSets.create("iosCommonTest") {
        dependsOn(sourceSets.getByName("commonTest"))
    }

    if (enableArm32) {
        sourceSets.getByName("iosArm32Main") { dependsOn(iosCommonMain) }
        sourceSets.getByName("iosArm32Test") { dependsOn(iosCommonTest) }
    }
    sourceSets.getByName("iosArm64Main") { dependsOn(iosCommonMain) }
    sourceSets.getByName("iosArm64Test") { dependsOn(iosCommonTest) }
    sourceSets.getByName("iosX64Main") { dependsOn(iosCommonMain) }
    sourceSets.getByName("iosX64Test") { dependsOn(iosCommonTest) }
    sourceSets.getByName("iosSimulatorArm64Main") { dependsOn(iosCommonMain) }
    sourceSets.getByName("iosSimulatorArm64Test") { dependsOn(iosCommonTest) }
}

fun KotlinMultiplatformExtension.tvosAll() {
    tvosAll { }
}

fun KotlinMultiplatformExtension.tvosAll(configure: Action<KotlinNativeTarget>) {
    tvosArm64(configure)
    tvosX64 { configure.execute(this) }
    tvosSimulatorArm64 { configure.execute(this) }

    val tvosCommonMain = sourceSets.create("tvosCommonMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    val tvosCommonTest = sourceSets.create("tvosCommonTest") {
        dependsOn(sourceSets.getByName("commonTest"))
    }

    sourceSets.getByName("tvosArm64Main") { dependsOn(tvosCommonMain) }
    sourceSets.getByName("tvosArm64Test") { dependsOn(tvosCommonTest) }
    sourceSets.getByName("tvosX64Main") { dependsOn(tvosCommonMain) }
    sourceSets.getByName("tvosX64Test") { dependsOn(tvosCommonTest) }
    sourceSets.getByName("tvosSimulatorArm64Main") { dependsOn(tvosCommonMain) }
    sourceSets.getByName("tvosSimulatorArm64Test") { dependsOn(tvosCommonTest) }
}

fun KotlinMultiplatformExtension.watchosAll() {
    watchosAll { }
}

fun KotlinMultiplatformExtension.watchosAll(configure: Action<KotlinNativeTarget>) {
    watchosArm64(configure)
    watchosX64 { configure.execute(this) }
    watchosSimulatorArm64 { configure.execute(this) }

    val watchosCommonMain = sourceSets.create("watchosCommonMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    val watchosCommonTest = sourceSets.create("watchosCommonTest") {
        dependsOn(sourceSets.getByName("commonTest"))
    }

    sourceSets.getByName("watchosArm64Main") { dependsOn(watchosCommonMain) }
    sourceSets.getByName("watchosArm64Test") { dependsOn(watchosCommonTest) }
    sourceSets.getByName("watchosX64Main") { dependsOn(watchosCommonMain) }
    sourceSets.getByName("watchosX64Test") { dependsOn(watchosCommonTest) }
    sourceSets.getByName("watchosSimulatorArm64Main") { dependsOn(watchosCommonMain) }
    sourceSets.getByName("watchosSimulatorArm64Test") { dependsOn(watchosCommonTest) }
}

fun KotlinMultiplatformExtension.macosAll() {
    macosAll { }
}

fun KotlinMultiplatformExtension.macosAll(configure: Action<KotlinNativeTargetWithHostTests>) {
    macosX64(configure)
    macosArm64(configure)

    val macosCommonMain = sourceSets.create("macosCommonMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    val macosCommonTest = sourceSets.create("macosCommonTest") {
        dependsOn(sourceSets.getByName("commonTest"))
    }

    sourceSets.getByName("macosX64Main") { dependsOn(macosCommonMain) }
    sourceSets.getByName("macosX64Test") { dependsOn(macosCommonTest) }
    sourceSets.getByName("macosArm64Main") { dependsOn(macosCommonMain) }
    sourceSets.getByName("macosArm64Test") { dependsOn(macosCommonTest) }
}

fun KotlinMultiplatformExtension.linuxAll() {
    linuxAll { }
}

fun KotlinMultiplatformExtension.linuxAll(configure: Action<KotlinNativeTarget>) {
    linuxX64 { configure.execute(this) }
    // TODO: enable when supported by atomicfu
    //linuxArm64(configure)

    val linuxCommonMain = sourceSets.create("linuxCommonMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    val linuxCommonTest = sourceSets.create("linuxCommonTest") {
        dependsOn(sourceSets.getByName("commonTest"))
    }

    sourceSets.getByName("linuxX64Main") { dependsOn(linuxCommonMain) }
    sourceSets.getByName("linuxX64Test") { dependsOn(linuxCommonTest) }
    //sourceSets.getByName("linuxArm64Main") { dependsOn(linuxCommonMain) }
    //sourceSets.getByName("linuxArm64Test") { dependsOn(linuxCommonTest) }
}

fun Project.androidLib(namespace: String, configure: Action<LibraryExtension> = Action { }) {
    (this as ExtensionAware).extensions.configure<LibraryExtension>("android") {
        configure.execute(this)
        this.namespace = namespace
        compileSdk = 33
        defaultConfig {
            minSdk = 21
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
}
