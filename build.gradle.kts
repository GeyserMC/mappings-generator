import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.StandardCopyOption

val javaMinecraftVersion = "1.20.2"
val bedrockResourcePackVersion = "1.20.50"
val resourcePack = file("bedrockresourcepack.zip")
val bedrockSamples = file("bedrock-samples.zip")

group = "org.geysermc.mappings-generator"
version = "1.1.0"

plugins {
    java
    `maven-publish`
    id("org.spongepowered.gradle.vanilla")
}

dependencies {
    implementation("org.projectlombok", "lombok", "1.18.20")

    implementation("org.mockito", "mockito-core", "3.+")

    implementation("org.reflections", "reflections", "0.9.12")

    implementation("org.cloudburstmc.protocol", "bedrock-connection", "3.0.0.Beta1-SNAPSHOT")

    annotationProcessor("org.projectlombok", "lombok", "1.18.20")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
}

minecraft {
    // https://github.com/SpongePowered/Sponge/blob/3cb480a347a33a424797c0e8f36b91cd1437d21d/build.gradle.kts
    version(javaMinecraftVersion)
    platform(org.spongepowered.gradle.vanilla.repository.MinecraftPlatform.CLIENT)
    project.sourceSets["main"].resources
        .filter { it.name.endsWith(".accesswidener") }
        .files
        .forEach {
            accessWideners(it)
            parent?.minecraft?.accessWideners(it)
        }
}

val samplesTask = tasks.register<DownloadFileTask>("downloadBedrockSamples") {
    url.set("https://github.com/Mojang/bedrock-samples/archive/refs/tags/v${bedrockResourcePackVersion}.zip")
    destination.set(bedrockSamples)
}
val resourcePackTask = tasks.register<CreateResourcePackTask>("resourcePack") {
    dependsOn(samplesTask)
    bedrockSamples.set(samplesTask.get().destination)
    packFile.set(resourcePack)
}

abstract class CreateResourcePackTask : DefaultTask() {

    @get:InputFile
    abstract val bedrockSamples: RegularFileProperty

    @get:OutputFile
    abstract val packFile: RegularFileProperty

    @TaskAction
    fun greet() {
        val samples = bedrockSamples.get().asFile.toPath()
        val output = packFile.get().asFile.toPath()
        Files.copy(samples, output, StandardCopyOption.REPLACE_EXISTING)

        FileSystems.newFileSystem(output)
            .use { fileSystem ->
                val root = fileSystem.rootDirectories.first()!!

                // the root just has one folder, eg "bedrock-samples-1.19.80.2"
                val subFolder = Files.walk(root, 1)
                    .filter { e -> e.toString().contains("bedrock-samples") }
                    .findFirst().get()

                val pack = subFolder.resolve("resource_pack")

                // move the resource pack contents to the root
                Files.walk(pack).use { stream ->
                    stream.filter { e -> e != pack }
                        .forEach { e ->
                            // order is important here so that empty destination directories are created first
                            Files.move(e, root.resolve(pack.relativize(e)))
                        }
                }

                // delete everything in the old folder, including itself
                Files.walk(subFolder).use { stream ->
                    stream.sorted(Comparator.reverseOrder()) // delete files before their parent directories
                        .forEach(Files::delete)
                }
            }
    }
}

abstract class DownloadFileTask : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val destination: RegularFileProperty

    @TaskAction
    fun greet() {
        val file = destination.asFile.get()
        val url = URL(this.url.get())

        println("Downloading $url to $file")

        Channels.newChannel(url.openStream()).use { channel ->
            file.outputStream().use { outputStream ->
                outputStream.channel.transferFrom(channel, 0, Long.MAX_VALUE)
            }
        }

        println("Download of $url complete!")
    }
}