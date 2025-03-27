import java.io.File

// =========================================================================
// Global Plugins & Repositories
// =========================================================================

plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

// =========================================================================
// Subprojects Configuration
// =========================================================================

subprojects {

    // ----------------------------------
    // Base Plugin Application
    // ----------------------------------

    // Apply the Java plugin to every subproject
    plugins.apply("java")

    // Ignore the shared library
    if (name != "shared") {
        plugins.apply("application")
        plugins.apply("com.github.johnrengelman.shadow")
    }

    // ----------------------------------
    // Java Toolchain & Compatibility
    // ----------------------------------

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // ----------------------------------
    // Testing Configuration
    // ----------------------------------

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = true
        }
    }

    // ----------------------------------
    // Packaging Configuration
    // ----------------------------------

    // Disable the default jar task, as ShadowJar is used for packaging
    tasks.withType<Jar> {
        enabled = false
    }

    // ----------------------------------
    // ShadowJar Packaging
    // ----------------------------------

    afterEvaluate {
        if (name != "shared") {

            // Utility function to resolve and validate required directories
            fun requireDir(relativePath: String): File {
                val dir = File(rootProject.projectDir, relativePath)
                if (!dir.exists()) {
                    throw GradleException("Required directory not found: ${dir.absolutePath}")
                }
                return dir
            }

            // Configure ShadowJar tasks provided by the shadow plugin
            tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
                mergeServiceFiles()
                archiveClassifier.set("")
                destinationDirectory.set(file("$rootDir/shadowjar"))

                if (name == "server") {
                    from(requireDir("../assets/world-maps")) {
                        into("maps")
                    }
                }

                from(File("../assets/licenses").absolutePath) {
                    into("licenses")
                }

                // Validate and copy the LICENSE file into licenses/LICENSE
                val licenseFile = File(rootProject.projectDir, "../LICENSE")
                if (!licenseFile.exists()) {
                    throw GradleException("Required LICENSE file not found: ${licenseFile.absolutePath}")
                }

                from(licenseFile) {
                    into("licenses")
                    rename { "LICENSE" }
                }
            }

            // Make the "build" task depend on the ShadowJar creation
            tasks.named("build") {
                dependsOn(tasks.named("shadowJar"))
            }
        }
    }
}
