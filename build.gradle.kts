import org.cadixdev.atlas.Atlas
import org.cadixdev.bombe.asm.jar.JarEntryRemappingTransformer
import org.cadixdev.bombe.jar.JarClassEntry
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.lorenz.asm.LorenzRemapper
import org.cadixdev.lorenz.io.proguard.ProGuardReader

import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths

val serverJarHash = "054b2065dd63c3e4227879046beae7acaeb7e8d3"
val serverMappingsHash = "8d6960e996a40b8350f5973d8a237469a9a6a7bc"
val serverJarVersion = "21w20a"

group = "org.geysermc.mappings-generator"
version = "1.1.0"

plugins {
    java
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

val deobfuscateMinecraftJar = tasks.register<DeobfuscateJarTask>("deobfuscateMinecraftJar") {
    version = serverJarVersion
}

val publishJarToMavenLocal = tasks.register<PublishJarToMavenLocalTask>("publishJarToMavenLocal") {
    version = serverJarVersion
}

tasks.register("installServerJar") {
    // Not sure if this is the best way to do it but it works...
    downloadMinecraftJar.get().greet()
    downloadMappings.get().greet()
    deobfuscateMinecraftJar.get().greet()
    publishJarToMavenLocal.get().greet()
}

open class DownloadFileTask : DefaultTask() {

    @Internal var url: String? = null
    @Internal var fileLocation: String? = null

    @TaskAction
    fun greet() {
        println("Downloading file ${fileLocation}...")

        val url = URL(url)
        val channel = Channels.newChannel(url.openStream())

        val outputStream = FileOutputStream(fileLocation!!)
        outputStream.channel.transferFrom(channel, 0, Long.MAX_VALUE)

        println("Download of $fileLocation complete!")
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

open class PublishJarToMavenLocalTask : DefaultTask() {

    @Internal var version: String? = null

    @TaskAction
    fun greet() {
        val mvn = if (System.getProperty("os.name").toLowerCase().contains("windows")) "mvn.cmd" else "mvn"

        println("Publishing to maven local...")
        project.exec {
            commandLine(mvn, "install:install-file", "-Dfile=${version}-server-deobfuscated.jar", "-DgroupId=net.minecraft", "-DartifactId=server", "-Dversion=${version}", "-Dpackaging=jar")
        }
        println("Maven local publishing complete!")
    }
}

open class InstallServerJarTask : DefaultTask() {

    @Internal var minecraftJar: TaskProvider<*>? = null
    @Internal var mappings: TaskProvider<*>? = null
    @Internal var deobfuscateJar: TaskProvider<*>? = null
    @Internal var publish: TaskProvider<*>? = null

    @TaskAction
    fun greet() {
        dependsOn(minecraftJar)
        dependsOn(mappings)
        dependsOn(deobfuscateJar)
        dependsOn(publish)
    }
}