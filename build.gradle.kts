import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.StandardCopyOption

val javaMinecraftVersion = "1.21.11-pre2"
val bedrockResourcePackVersion = "1.21.130.28-preview"
val resourcePack = file("bedrockresourcepack.zip")
val bedrockSamples = file("bedrock-samples.zip")

group = "org.geysermc.mappings-generator"
version = "1.1.0"

plugins {
    java
    application
    `maven-publish`
    id("org.spongepowered.gradle.vanilla")
}

dependencies {
    implementation("org.projectlombok", "lombok", "1.18.30")

    implementation("org.mockito", "mockito-core", "3.+")

    implementation("org.cloudburstmc.protocol", "bedrock-codec", "3.0.0.Beta8-SNAPSHOT")
    implementation("org.cloudburstmc.protocol", "bedrock-connection", "3.0.0.Beta8-SNAPSHOT")

    implementation("org.cloudburstmc", "block-state-updater", "1.21.60-SNAPSHOT")

    implementation("org.apache.commons", "commons-text", "1.12.0")

    annotationProcessor("org.projectlombok", "lombok", "1.18.30")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_21
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

val blockPaletteTask = tasks.register<DownloadFileTask>("downloadBlockPalette") {
    url.set("https://raw.githubusercontent.com/CloudburstMC/Data/master/block_palette.nbt")
    destination.set(file("palettes/block_palette.nbt"))
}

val runtimeItemStatesTask = tasks.register<DownloadFileTask>("downloadRuntimeItemStates") {
    url.set("https://raw.githubusercontent.com/CloudburstMC/Data/master/runtime_item_states.json")
    destination.set(file("palettes/runtime_item_states.json"))
}

val itemComponentsTask = tasks.register<DownloadFileTask>("downloadItemComponents") {
    url.set("https://raw.githubusercontent.com/CloudburstMC/Data/master/item_components.nbt")
    destination.set(file("palettes/item_components.nbt"))
}

val downloadAll = tasks.register("downloadAll") {
    dependsOn(resourcePackTask, blockPaletteTask, runtimeItemStatesTask, itemComponentsTask)
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

application {
    mainClass.set("org.geysermc.generator.Main")
}