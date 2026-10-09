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
    exclude("**/RealFusionArtifactEncodeTest.class")
    exclude("**/RealFusionArtifactDecodeTest.class")
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

val isolatedFusionArtifact = layout.buildDirectory.file("fusion-isolated/rafaam-fused.json")
val isolatedFusionReport = layout.buildDirectory.file("fusion-isolated/decode-measurement.properties")

tasks.register<Test>("realFusionArtifactEncodeTest") {
    description = "Builds and streams the private Rafaam fused artifact in an isolated 512 MiB JVM"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealFusionArtifactEncodeTest.class")
    maxHeapSize = "512m"
    outputs.file(isolatedFusionArtifact)
    outputs.upToDateWhen { false }
    val fixtureDir = providers.gradleProperty("hsFusionFixtureDir")
    doFirst {
        require(fixtureDir.isPresent) { "Pass -PhsFusionFixtureDir=<private fixture directory>" }
        systemProperty("hsFusionFixtureDir", fixtureDir.get())
        systemProperty("hsFusionArtifactPath", isolatedFusionArtifact.get().asFile.absolutePath)
    }
}

tasks.register<Test>("realFusionArtifactLifecycleTest") {
    description = "Decodes and measures the private fused artifact in a fresh isolated 512 MiB JVM"
    group = "verification"
    dependsOn("realFusionArtifactEncodeTest")
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealFusionArtifactDecodeTest.class")
    maxHeapSize = "512m"
    inputs.file(isolatedFusionArtifact)
    outputs.file(isolatedFusionReport)
    outputs.upToDateWhen { false }
    doFirst {
        systemProperty("hsFusionArtifactPath", isolatedFusionArtifact.get().asFile.absolutePath)
        systemProperty("hsFusionMeasurementPath", isolatedFusionReport.get().asFile.absolutePath)
    }
}
