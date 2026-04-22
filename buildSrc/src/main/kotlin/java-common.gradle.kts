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
        palantirJavaFormat("2.50.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
