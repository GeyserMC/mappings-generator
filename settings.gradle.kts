rootProject.name = "mappings-generator"

// See https://github.com/SpongePowered/Sponge/blob/3cb480a347a33a424797c0e8f36b91cd1437d21d/settings.gradle.kts as a reference
pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }

    plugins {
        id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
    }
}

plugins {
    id("org.spongepowered.gradle.vanilla")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        mavenLocal()

        maven(url = "https://repo.opencollab.dev/maven-releases/")
        maven(url = "https://repo.opencollab.dev/maven-snapshots/")
        maven(url = "https://repo.spongepowered.org/repository/maven-public/")
    }
}