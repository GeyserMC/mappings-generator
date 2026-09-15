// See https://github.com/FabricMC/fabric-loom/blob/ad89ffdb4e3c6fb1647e45cb5b3ca87ff1e77803/src/main/java/net/fabricmc/loom/task/launch/GenerateDLIConfigTask.java#L183
// We want fancy ANSI colours in CI
if (providers.environmentVariable("CI").isPresent) {
    project.rootDir.resolve(".project").createNewFile()
}

plugins {
    alias(libs.plugins.fabric.loom)
}

group = "org.geysermc.mappings-generator"
version = "2.0.2"

val targetJavaVersion = 25

// Matches xx.xx.xx, discards preview info
val humanBedrockVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()

val minecraftJavaVersion = libs.versions.minecraft.java
val minecraftBedrockVersion = libs.versions.minecraft.bedrock.tag
val minecraftBedrockHumanVersion = minecraftBedrockVersion.map { humanBedrockVersionRegex.find(it)?.value!! }

// Have to do this to explicitly attach the Mockito Java agent: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()

    maven("https://maven.fabricmc.net") {
        name = "Fabric"
    }

    maven("https://repo.opencollab.dev/main") {
        name = "OpenCollab"
    }
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    implementation(libs.commons.text)
    implementation(libs.mockito.core)
    implementation(libs.bundles.cloudburst.protocol)

    mockitoAgent(libs.mockito.core) {
        isTransitive = false
    }
}

java {
    val version = JavaVersion.toVersion(targetJavaVersion)

    toolchain {
        languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }

    sourceCompatibility = version
    targetCompatibility = version
}

fabricApi {
    configureDataGeneration {
        outputDirectory = file("mappings")
        addToResources = false
        client = true
    }
}

loom {
    runConfigs {
        named("datagen") {
            jvmArguments.add("-Dline.separator=\u000a")
            jvmArguments.add("-javaagent:${mockitoAgent.asPath}")
        }

        register("MCPL") {
            inherit(getByName("datagen"))
            jvmArguments.add("-Dgeyser.providers.selected=mcpl")
        }

        register("javaclass") {
            inherit(getByName("datagen"))
            jvmArguments.add("-Dgeyser.providers.selected=javaclass")
        }

        register("mappings") {
            inherit(getByName("datagen"))
            jvmArguments.add("-Dgeyser.providers.selected=mappings")
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.release = targetJavaVersion
    }

    getByName("runClient") {
        enabled = false
    }

    getByName("runClientRenderDoc") {
        enabled = false
    }

    getByName("runServer") {
        enabled = false
    }

    processResources {
        inputs.property("version", version)
        inputs.property("minecraft_version", minecraftJavaVersion.get())
        inputs.property("loader_version", libs.versions.fabric.loader.get())
        inputs.property("bedrock_version", minecraftBedrockVersion.get())
        inputs.property("human_bedrock_version", minecraftBedrockHumanVersion.get())
        inputs.property("bedrock_data_sha", libs.versions.minecraft.bedrock.data.get())
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to version,
                    "minecraft_version" to minecraftJavaVersion.get(),
                    "loader_version" to libs.versions.fabric.loader.get(),
                    "bedrock_version" to minecraftBedrockVersion.get(),
                    "human_bedrock_version" to minecraftBedrockHumanVersion.get(),
                    "bedrock_data_sha" to libs.versions.minecraft.bedrock.data.get()
                )
            )
        }
    }

    val prepareFullRelease = register<Zip>("prepareFullRelease") {
        description = "Zips the contents of the \"mappings\" folder to build/mappings"

        dependsOn("runDatagen")

        from("mappings")
        // Move mappings README and LICENSE to root
        filesMatching(setOf("mappings/README.md", "mappings/LICENSE")) {
            path = relativePath.lastName
        }

        // Exclude datagen cache
        exclude(".cache")
        // Exclude bedrock-data.zip and bedrock-samples.zip
        exclude("*.zip")

        destinationDirectory = project.layout.buildDirectory.dir("mappings")

        archiveBaseName = "mappings"
        archiveVersion = "v${version}"
        archiveClassifier = "full"
    }

    val prepareMinRelease = register<Zip>("prepareMinRelease") {
        description = "Zips the contents of the \"mappings/mappings\" folder to build/mappings"

        dependsOn("runDatagen")

        from("mappings/mappings")

        destinationDirectory = project.layout.buildDirectory.dir("mappings")

        archiveBaseName = "mappings"
        archiveVersion = "v${version}"
        archiveClassifier = "min"
    }

    val writeVersionsToGithubOutput = register("writeVersionsToGithubOutput") {
        description = "Writes version information to GITHUB_OUTPUT, if it exists"

        doLast {
            val githubOutput = providers.environmentVariable("GITHUB_OUTPUT").map { file(it) }
            if (githubOutput.isPresent) {

                // Hack: tags should always start with version, if it doesn't then the version was bumped, so reset build number
                val buildNumber = providers.environmentVariable("LAST_RELEASE_TAG").map { if (it.startsWith(version.toString())) "auto" else "0" }.orElse("auto")
                githubOutput.get().writeText(
                    "version=${version}\n" +
                    "java_version=${minecraftJavaVersion.get()}\n" +
                    "bedrock_version=${minecraftBedrockHumanVersion.get()}\n" +
                    "full_file=${prepareFullRelease.get().archiveFile.get().asFile.absolutePath}\n" +
                    "min_file=${prepareMinRelease.get().archiveFile.get().asFile.absolutePath}\n" +
                    "build_number=${buildNumber.get()}\n"
                )
            }
        }
    }

    val prepareRelease = register("prepareRelease") {
        description = "Creates the ZIP files to be published for release"

        dependsOn(prepareFullRelease)
        dependsOn(prepareMinRelease)
        dependsOn(writeVersionsToGithubOutput)
    }
}
