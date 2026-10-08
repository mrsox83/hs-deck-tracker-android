import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    // The app brings the Android build of ONNX Runtime, which has the same API
    compileOnly(libs.onnxruntime)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.onnxruntime)
}

tasks.test {
    exclude("**/RealFusionFixtureTest.class")
}

tasks.register<Test>("realFusionFixtureTest") {
    description = "Runs private mobile fusion fixture assertions from -PhsFusionFixtureDir"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealFusionFixtureTest.class")
    val fixtureDir = providers.gradleProperty("hsFusionFixtureDir")
    doFirst {
        require(fixtureDir.isPresent) { "Pass -PhsFusionFixtureDir=<private fixture directory>" }
        systemProperty("hsFusionFixtureDir", fixtureDir.get())
    }
}
