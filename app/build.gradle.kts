// =========================================================================
// Global plugins & repos
// =========================================================================

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

// =========================================================================
// Main
// =========================================================================

disableRootShadowjar()
subprojects {
    applyPlugins()
    configureJava()
    configureTesting()
    disableJarTask()

    afterEvaluate {
        configureShadowjar()
    }
}

// =========================================================================
// Configuration methods
// =========================================================================

fun Project.applyPlugins() {
    apply(plugin = "java")
    if (project.name != "shared") {
        apply(plugin = "application")
        apply(plugin = "com.github.johnrengelman.shadow")
    }
}

fun Project.configureJava() {
    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

fun Project.configureTesting() {
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = true
        }
    }
}

fun Project.disableJarTask() {
    tasks.withType<Jar> {
        enabled = false
    }
}

fun disableRootShadowjar() {
    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
        enabled = false
    }
}

fun Project.configureShadowjar() {
    if (project.name == "shared") return

    val mapsDir = File(rootProject.projectDir, "../maps")
    val licensesDir = File(rootProject.projectDir, "../licenses")

    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        mergeServiceFiles()
        archiveClassifier.set("")
        destinationDirectory.set(file("$rootDir/shadowjar"))
        from(project(":shared").sourceSets["main"].output) // Include shared lib
        doLast {
            println("Shadow JAR has been created at: ${archiveFile.get().asFile.absolutePath}")
        }

        if (name == "server") {
            from(requireDir("../assets/world-maps")) {
                into("maps")
            }
        }
        from(File("../assets/licenses").absolutePath) {
            into("licenses")
        }
        addLicenseFile()
    }

    tasks.named("build") {
        dependsOn(tasks.named("shadowJar"))
    }
}

// =========================================================================
// Private
// =========================================================================

fun com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.addLicenseFile() {
    val licenseFile = File(rootProject.projectDir, "../LICENSE")
    if (!licenseFile.exists()) {
        throw GradleException("Required LICENSE file not found: ${licenseFile.absolutePath}")
    }
    from(licenseFile) {
        into("licenses")
        rename { "LICENSE" }
    }
}

fun requireDir(relativePath: String): File {
    val dir = File(rootProject.projectDir, relativePath)
    if (!dir.exists()) {
        throw GradleException("Required directory not found: ${dir.absolutePath}")
    }
    return dir
}
