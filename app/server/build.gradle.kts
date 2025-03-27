// =========================================================================
// Plugins & Dependencies
// =========================================================================

plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":shared"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.google.protobuf:protobuf-java:3.25.1")
}

// =========================================================================
// Application Configuration
// =========================================================================

application {
    mainClass.set("flaggi.server.Server")
}

// =========================================================================
// ShadowJar Packaging Configuration
// =========================================================================

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    from(project(":shared").sourceSets["main"].output)

    archiveBaseName.set("Flaggi-server")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("")

    // Output directory for the shadow JAR
    destinationDirectory.set(file("$rootDir/shadowjar"))

    doLast {
        println("Server Shadow JAR created at: ${archiveFile.get().asFile.absolutePath}")
    }
}

// =========================================================================
// Build Task Dependency Configuration
// =========================================================================

tasks.build {
    dependsOn(tasks.shadowJar)
}
