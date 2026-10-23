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
    exclude("**/RealCoachingHandPackageTest.class")
    exclude("**/RealDiscoveryFixtureTest.class")
    exclude("**/RealPendingPairingTest.class")
    exclude("**/RealManualReconnectTest.class")
    exclude("**/RealPostMatchCollectionTest.class")
    exclude("**/RealFusionFixtureTest.class")
    exclude("**/RealFusionArtifactEncodeTest.class")
    exclude("**/RealFusionArtifactDecodeTest.class")
    exclude("**/RealVisualFieldFixtureTest.class")
    exclude("**/RealCardCatalogFixtureTest.class")
}

tasks.register<Test>("realDiscoveryFixtureTest") {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealDiscoveryFixtureTest.class")
    maxHeapSize = "512m"
    outputs.upToDateWhen { false }
    doFirst {
        val directory = providers.gradleProperty("hsDiscoveryFixtureDir")
        require(directory.isPresent) { "Pass -PhsDiscoveryFixtureDir=<private fixture directory>" }
        systemProperty("hsDiscoveryFixtureDir", directory.get())
    }
}

tasks.register<Test>("realPendingPairingTest") {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealPendingPairingTest.class")
    maxHeapSize = "512m"
    outputs.upToDateWhen { false }
    doFirst {
        for (name in listOf("hsPendingZip", "hsPendingTracker")) {
            val value = providers.gradleProperty(name)
            require(value.isPresent) { "Pass -P$name=<private fixture file>" }
            systemProperty(name, value.get())
        }
    }
}

tasks.register<Test>("realManualReconnectTest") {
    description = "Checks the owner's private manual-record reconnect and partial coverage warning"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealManualReconnectTest.class")
    maxHeapSize = "512m"
    outputs.upToDateWhen { false }
    doFirst {
        for (name in listOf("hsManualReconnectZip", "hsManualReconnectTracker")) {
            val value = providers.gradleProperty(name)
            require(value.isPresent) { "Pass -P$name=<private fixture file>" }
            systemProperty(name, value.get())
        }
    }
}

tasks.register<Test>("realPostMatchCollectionTest") {
    description = "Rebuilds the pinned private reconnect exporter bundle using the R5 parser/journal"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealPostMatchCollectionTest.class")
    maxHeapSize = "512m"
    outputs.upToDateWhen { false }
    val fixture = providers.gradleProperty("hsPostMatchFixtureZip")
    doFirst {
        require(fixture.isPresent) { "Pass -PhsPostMatchFixtureZip=<pinned R3 reconnect ZIP>" }
        systemProperty("hsPostMatchFixtureZip", fixture.get())
    }
}

tasks.register<Test>("realCardCatalogFixtureTest") {
    description = "Parses a downloaded full card catalog and resolves the owner's exact deck"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealCardCatalogFixtureTest.class")
    outputs.upToDateWhen { false }
    val catalog = providers.gradleProperty("hsFullCardCatalog")
    doFirst {
        require(catalog.isPresent) { "Pass -PhsFullCardCatalog=<downloaded full catalog JSON>" }
        systemProperty("hsFullCardCatalog", catalog.get())
    }
}

tasks.register<Test>("realVisualFieldFixtureTest") {
    description = "Replays labeled private R4 diagnostic keyframes from -PhsVisualFixtureZip"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealVisualFieldFixtureTest.class")
    outputs.upToDateWhen { false }
    val fixtureZip = providers.gradleProperty("hsVisualFixtureZip")
    val expandedZip = providers.gradleProperty("hsVisualExpandedFixtureZip")
    val resourceZip = providers.gradleProperty("hsVisualResourceFixtureZip")
    doFirst {
        require(fixtureZip.isPresent) { "Pass -PhsVisualFixtureZip=<private labeled diagnostic ZIP>" }
        systemProperty("hsVisualFixtureZip", fixtureZip.get())
        require(expandedZip.isPresent) { "Pass -PhsVisualExpandedFixtureZip=<labeled expanded-hand ZIP>" }
        systemProperty("hsVisualExpandedFixtureZip", expandedZip.get())
        require(resourceZip.isPresent) { "Pass -PhsVisualResourceFixtureZip=<labeled v5 resource ZIP>" }
        systemProperty("hsVisualResourceFixtureZip", resourceZip.get())
    }
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

tasks.register<Test>("realCoachingHandPackageTest") {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/RealCoachingHandPackageTest.class")
    maxHeapSize = "1g"
    doFirst {
        val reviewPackage = providers.gradleProperty("hsHandReviewPackage")
        require(reviewPackage.isPresent) { "Pass -PhsHandReviewPackage=<private review ZIP>" }
        systemProperty("hsHandReviewPackage", reviewPackage.get())
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
