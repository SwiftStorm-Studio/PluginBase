pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.0.20"
        id("fabric-loom") version "1.8-SNAPSHOT" apply false
        id("cl.franciscosolis.sonatype-central-upload") version "1.0.3" apply false
        `maven-publish` apply false
    }
}
rootProject.name = "swiftbase"

include(":integrations")

include(":integrations:core")
project(":integrations:core").name = "swiftbase-core"

include(":integrations:fabric")
project(":integrations:fabric").name = "swiftbase-fabric"

include(":integrations:paper")
project(":integrations:paper").name = "swiftbase-paper"