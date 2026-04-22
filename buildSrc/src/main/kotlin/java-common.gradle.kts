plugins {
    `java`
    id("com.diffplug.spotless")
}

repositories { mavenCentral() }

dependencies { implementation("com.google.protobuf:protobuf-java:3.25.1") }

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

spotless {
    java {
        target("src/*/java/**/*.java")
        targetExclude("**/build/**")
        googleJavaFormat("1.25.2")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
