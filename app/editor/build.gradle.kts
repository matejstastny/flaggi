// =========================================================================
// Plugins & Dependencies
// =========================================================================

plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":shared"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
}

// =========================================================================
// Application Configuration
// =========================================================================
application {
    mainClass.set("flaggieditor.App")
}

// =========================================================================
// ShadowJar Packaging Configuration
// =========================================================================

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    from(project(":shared").sourceSets["main"].output)

    // Set archive naming and version details
    archiveBaseName.set("Flaggi-editor")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("")

    // Output directory for the shadow JAR
    destinationDirectory.set(file("$rootDir/shadowjar"))

    // Log the output location after packaging
    doLast {
        println("Editor Shadow JAR created at: ${archiveFile.get().asFile.absolutePath}")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
