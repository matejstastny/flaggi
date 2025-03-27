// Global plugins & repos --------------------------------------------------------------------

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

// Subproject settings  ----------------------------------------------------------------------

subprojects {
    apply(plugin = "java")

    if (project.name != "shared") {
        apply(plugin = "application")
        apply(plugin = "com.github.johnrengelman.shadow")
    }

    // Java configuration
    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }


    // Tests configuration
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = true
        }
    }

    // Disable jar task (shadowJar will be used instead)
    tasks.withType<Jar> {
        enabled = false
    }

    // ShadowJar Configuration
    afterEvaluate {
        if (project.name != "shared") {
            val mapsDir = File(rootProject.projectDir, "../maps")
            val licensesDir = File(rootProject.projectDir, "../licenses")
            tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
                mergeServiceFiles()
                archiveClassifier.set("")
                destinationDirectory.set(file("$rootDir/shadowjar"))
                doLast {
                    println("Shadow JAR has been created at: ${archiveFile.get().asFile.absolutePath}")
                }
                if (project.name == "server") {
                    from(mapsDir) {
                        into("maps")
                    }
                }
                from(licensesDir) {
                    into("licenses")
                }
            }
            tasks.named("build") {
                dependsOn(tasks.named("shadowJar"))
            }
        }
    }
}
