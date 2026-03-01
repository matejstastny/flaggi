plugins {
    id("java-common")
    id("com.gradleup.shadow") version "9.2.2"
    id("com.google.protobuf") version "0.9.5"
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("org.json:json:20230618")
}

protobuf {
    // https://mvnrepository.com/artifact/com.google.protobuf/protoc
    protoc { artifact = "com.google.protobuf:protoc:3.25.1" }
    generateProtoTasks { all().configureEach { builtins { named("java") } } }
}

tasks.shadowJar {
    enabled = false
}
