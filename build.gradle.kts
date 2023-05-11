import java.net.URL
import java.nio.channels.Channels

val javaMinecraftVersion = "1.20-pre1"
val bedrockResourcePackVersion = "1.19.0.34"

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
    implementation("com.nukkitx", "nbt", "2.0.2")
    implementation("com.nukkitx.protocol", "bedrock-common", "2.9.4-SNAPSHOT")

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

val downloadResourcePack = tasks.register<DownloadFileTask>("downloadResourcePack") {
    url = "https://void.bedrock.dev/resources/${bedrockResourcePackVersion}.zip"
    fileLocation = "bedrockresourcepack.zip"
}

open class DownloadFileTask : DefaultTask() {

    @Internal var url: String? = null
    @Internal var fileLocation: String? = null

    @TaskAction
    fun greet() {
        val file = File(fileLocation!!)
        if (!file.exists()) {
            println("Downloading file ${fileLocation}...")

            val url = URL(url)
            val channel = Channels.newChannel(url.openStream())

            val outputStream = file.outputStream()
            outputStream.channel.transferFrom(channel, 0, Long.MAX_VALUE)

            println("Download of $fileLocation complete!")
        }
    }
}