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

val serverJarHash = "4de310c8c4f4a8ab4574246c1d63e3de3af1444d"
val serverMappingsHash = "98a1398dc4144f92e10dd6967a231763741952e7"
val serverJarVersion = "1.17-rc1"

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
}

dependencies {
    implementation("org.projectlombok", "lombok", "1.18.4")

    implementation("org.reflections", "reflections", "0.9.12")
    implementation("com.nukkitx", "nbt", "2.0.2")
    implementation("com.google.code.gson", "gson", "2.8.5")
    implementation("net.minecraft", "server", serverJarVersion)

    annotationProcessor("org.projectlombok", "lombok", "1.18.20")
}

publishing {
    publications {
        create<MavenPublication>("serverJar") {
            groupId = "net.minecraft"
            artifactId = "server"
            version = serverJarVersion
            artifact("${serverJarVersion}-server-deobfuscated.jar")
        }
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_16
}

val downloadMinecraftJar = tasks.register<DownloadFileTask>("downloadMinecraftJar") {
    url = "https://launcher.mojang.com/v1/objects/${serverJarHash}/server.jar"
    fileLocation = "${serverJarVersion}-server.jar"
}

val downloadMappings = tasks.register<DownloadFileTask>("downloadMappings") {
    url = "https://launcher.mojang.com/v1/objects/${serverMappingsHash}/server.txt"
    fileLocation = "${serverJarVersion}-mappings.txt"
}

val downloadResourcePack = tasks.register<DownloadFileTask>("downloadResourcePack") {
    url = "https://void.bedrock.dev/resources/${bedrockResourcePackVersion}.zip"
    fileLocation = "bedrockresourcepack.zip"
}

val deobfuscateMinecraftJar = tasks.register<DeobfuscateJarTask>("deobfuscateMinecraftJar") {
    dependsOn("downloadMinecraftJar")
    dependsOn("downloadMappings")

    version = serverJarVersion
}

tasks.register("installServerJar") {
    mustRunAfter("deobfuscateMinecraftJar")
    dependsOn("deobfuscateMinecraftJar")
    dependsOn("publishServerJarPublicationToMavenLocal")
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
        println("Deobfuscating server jar...")

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
            atlas.run(Paths.get(dir, "${version}-server.jar"), Paths.get(dir, "${version}-server-deobfuscated.jar"))
        }

        println("Deobfuscation complete!")
    }
}