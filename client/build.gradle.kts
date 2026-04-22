plugins {
    id("java-common")
    id("application")
    id("com.gradleup.shadow") version "9.2.2"
    id("com.google.protobuf") version "0.9.5"
}

dependencies { implementation(project(":shared")) }

application { mainClass.set("flaggi.client.App") }

tasks.shadowJar {
    archiveBaseName = "flaggi-client"
    archiveVersion = ""
    archiveClassifier = ""
}

tasks.jar {
    enabled = false
}
