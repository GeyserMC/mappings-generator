import org.cadixdev.atlas.Atlas
import org.cadixdev.bombe.asm.jar.JarEntryRemappingTransformer
import org.cadixdev.bombe.jar.JarClassEntry
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.lorenz.asm.LorenzRemapper
import org.cadixdev.lorenz.io.proguard.ProGuardReader

import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths

val clientJarHash = "8d9b65467c7913fcf6f5b2e729d44a1e00fde150"
val clientMappingsHash = "e4d540e0cba05a6097e885dffdf363e621f87d3f"
val clientJarVersion = "1.17.1"

val bedrockResourcePackVersion = "1.17.0.2"

group = "org.geysermc.mappings-generator"
version = "1.1.0"

plugins {
    java
    `maven-publish`
}

buildscript {
    val asmVersion = "9.1"

    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    dependencies {
        classpath("org.cadixdev:lorenz-asm:0.5.7") {
            exclude("org.ow2.asm")
        }
        classpath("org.cadixdev:atlas:0.2.2") {
            exclude("org.ow2.asm")
        }
        classpath("org.cadixdev:lorenz-io-proguard:0.5.7")
        classpath("org.ow2.asm:asm-util:$asmVersion")
        classpath("org.ow2.asm:asm-tree:$asmVersion")
        classpath("org.ow2.asm:asm-commons:$asmVersion")
    }
}

repositories {
    mavenCentral()
    mavenLocal()

    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven(url = "https://repo.opencollab.dev/maven-releases/")
    maven(url = "https://repo.opencollab.dev/maven-snapshots/")
    maven(url = "https://libraries.minecraft.net/")
}

dependencies {
    implementation("org.projectlombok", "lombok", "1.18.4")

    implementation("org.reflections", "reflections", "0.9.12")
    implementation("com.nukkitx", "nbt", "2.0.2")
    implementation("com.google.code.gson", "gson", "2.8.5")
    implementation("net.minecraft", "client", clientJarVersion)

    implementation("com.mojang", "brigadier", "1.0.18")
    implementation("com.mojang", "datafixerupper", "4.0.26")
    implementation("com.mojang", "javabridge", "1.1.23")
    implementation("io.netty", "netty-all", "4.1.25.Final")
    implementation("it.unimi.dsi", "fastutil", "8.5.6")

    annotationProcessor("org.projectlombok", "lombok", "1.18.20")
}

publishing {
    publications {
        create<MavenPublication>("clientJar") {
            groupId = "net.minecraft"
            artifactId = "client"
            version = clientJarVersion
            artifact("${clientJarVersion}-client-deobfuscated.jar")
        }
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_16
}

val downloadMinecraftJar = tasks.register<DownloadFileTask>("downloadMinecraftJar") {
    url = "https://launcher.mojang.com/v1/objects/${clientJarHash}/client.jar"
    fileLocation = "${clientJarVersion}-client.jar"
}

val downloadMappings = tasks.register<DownloadFileTask>("downloadMappings") {
    url = "https://launcher.mojang.com/v1/objects/${clientMappingsHash}/client.txt"
    fileLocation = "${clientJarVersion}-mappings.txt"
}

val downloadResourcePack = tasks.register<DownloadFileTask>("downloadResourcePack") {
    url = "https://void.bedrock.dev/resources/${bedrockResourcePackVersion}.zip"
    fileLocation = "bedrockresourcepack.zip"
}

val deobfuscateMinecraftJar = tasks.register<DeobfuscateJarTask>("deobfuscateMinecraftJar") {
    dependsOn("downloadMinecraftJar")
    dependsOn("downloadMappings")

    version = clientJarVersion
}

tasks.register("installClientJar") {
    mustRunAfter("deobfuscateMinecraftJar")
    dependsOn("deobfuscateMinecraftJar")
    dependsOn("publishClientJarPublicationToMavenLocal")
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

open class DeobfuscateJarTask : DefaultTask() {

    @Internal var version: String? = null

    @TaskAction
    fun greet() {
        println("Deobfuscating client jar...")

        val dir = System.getProperty("user.dir")

        val mojangMappings = MappingSet.create()
        try {
            ProGuardReader(Files.newBufferedReader(Paths.get(dir, "${version}-mappings.txt"))).use { reader ->
                reader.read().reverse(mojangMappings)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Adapted from https://github.com/SpongePowered/Sponge/blob/2def9ef39130023a09ce800873ac163943bf92e8/vanilla/src/installer/java/org/spongepowered/vanilla/installer/InstallerMain.java#L301
        Atlas().use { atlas ->
            atlas.install { ctx ->
                object : JarEntryRemappingTransformer(LorenzRemapper(mojangMappings, ctx.inheritanceProvider())) {
                    override fun transform(entry: JarClassEntry): JarClassEntry? {
                        if (entry.name.startsWith("it/unimi")
                            || entry.name.startsWith("com/google")
                            || entry.name.startsWith("com/mojang/datafixers")
                            || entry.name.startsWith("com/mojang/brigadier")
                            || entry.name.startsWith("org/apache")) {
                            return entry
                        }
                        return super.transform(entry)
                    }
                }
            }
            atlas.run(Paths.get(dir, "${version}-client.jar"), Paths.get(dir, "${version}-client-deobfuscated.jar"))
        }

        println("Deobfuscation complete!")
    }
}