plugins { `java` }

repositories { mavenCentral() }

dependencies { implementation("com.google.protobuf:protobuf-java:3.25.1") }

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
