plugins {
    id("java-common")
    id("application")
    id("com.gradleup.shadow") version "9.2.2"
    id("com.google.protobuf") version "0.9.5"
}

dependencies { implementation(project(":shared")) }

application { mainClass.set("flaggi.server.Server") }

tasks.shadowJar {
  destinationDirectory = rootProject.layout.projectDirectory.dir("shadowjar")
  archiveBaseName = "flaggi-server"
  archiveClassifier = ""
  from(rootProject.layout.projectDirectory.file("LICENSE")) {
    into("licenses")
  }
}

tasks.jar {
    enabled = false
}
