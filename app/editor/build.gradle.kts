plugins {
    id("java-common")
    id("application")
    id("com.gradleup.shadow") version "9.2.2"
    id("com.google.protobuf") version "0.9.5"
}

dependencies { implementation(project(":shared")) }

application { mainClass.set("flaggieditor.App") }

tasks.shadowJar {
  destinationDirectory = rootProject.layout.projectDirectory.dir("shadowjar")
  archiveBaseName = "flaggi-editor"
  archiveClassifier = ""
}

tasks.jar {
    enabled = false
}
