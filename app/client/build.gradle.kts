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
    mainClass.set("flaggi.client.App")
}


// =========================================================================
// ShadowJar Packaging Configuration for Client
// =========================================================================

tasks.shadowJar {
    enabled = true
    archiveBaseName.set("Flaggi-client")
    archiveVersion.set("1.0.0")
}
