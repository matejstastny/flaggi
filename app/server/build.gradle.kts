// =========================================================================
// Plugins & Dependencies
// =========================================================================

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

tasks.shadowJar {
    enabled = true
    archiveBaseName.set("Flaggi-server")
    archiveVersion.set("1.0.0")

    doLast {
        println("Server Shadow JAR created at: ${archiveFile.get().asFile.absolutePath}")
    }
}
