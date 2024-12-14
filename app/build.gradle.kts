plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    // => shadowJar
    id("com.gradleup.shadow") version "8.3.5"
    java
}

// Use Java 17 as minimum
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}



repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // The logging implementation (Log4j Core + API)
    implementation("org.apache.logging.log4j:log4j-core:2.24.1")

    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.1")

    runtimeOnly("org.apache.logging.log4j:log4j-layout-template-json:2.24.1")

    // Yaml-Reader
    // initially 2.18.0
    // but downgraded to 2.17.2 due to bug https://github.com/diffplug/spotless/issues/2303
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")

    // Mustang-Library + validator
    // https://mvnrepository.com/artifact/org.mustangproject/library
    // 2.15

    implementation("org.mustangproject:library:2.15.0")
    implementation("org.mustangproject:validator:2.15.0:shaded")

    // FilenameUtils
    implementation("commons-io:commons-io:2.17.0")

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // This dependency is used by the application.
    implementation(libs.guava)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "simplemonatserechnung.App"
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer())
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

val mainClass = "simplemonatserechnung.App"

tasks.withType<Jar> {
    // set basename for normal and fat jar
    archiveBaseName.set("smer")
    manifest {
        attributes["Main-Class"] = mainClass
    }
}

// Version number of this utility
version = "0.1.1"

