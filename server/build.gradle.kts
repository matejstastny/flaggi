plugins {
    id("java-common")
    id("application")
    id("com.gradleup.shadow") version "9.2.2"
    id("com.google.protobuf") version "0.9.5"
}

dependencies {
    implementation(project(":shared"))
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("io.javalin:javalin:6.7.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

application { mainClass.set("flaggi.server.Server") }

tasks.shadowJar {
    archiveBaseName = "flaggi-server"
    archiveVersion = ""
    archiveClassifier = ""
    from(rootProject.layout.projectDirectory.file("LICENSE")) {
        into("licenses")
    }
}

tasks.jar {
    enabled = false
}
