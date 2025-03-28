// =========================================================================
// Plugins & Dependencies
// =========================================================================

dependencies {
    implementation(project(":shared"))
    // testImplementation(libs.junit.jupiter)
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
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

tasks.shadowJar {
    enabled = true
    archiveBaseName.set("Flaggi-editor")
    archiveVersion.set("1.0.0")
}
