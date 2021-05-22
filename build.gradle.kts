import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

val serverJarHash = "054b2065dd63c3e4227879046beae7acaeb7e8d3"
val serverMappingsHash = "8d6960e996a40b8350f5973d8a237469a9a6a7bc"
val serverJarVersion = "21w20a"
val enigmaVersion = "0.27.3"

group = "org.geysermc.mappings-generator"
version = "1.1.0"

plugins {
    java
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

val downloadEnigma = tasks.register<DownloadFileTask>("downloadEnigma") {
    url = "https://maven.fabricmc.net/cuchaz/enigma-cli/${enigmaVersion}/enigma-cli-${enigmaVersion}-all.jar"
    fileLocation = "enigma-cli-${enigmaVersion}-all.jar"
}

val deobfuscateMinecraftJar = tasks.register<RunEnigmaTask>("deobfuscateMinecraftJar") {
    version = serverJarVersion
    enigma = enigmaVersion
}

val publishJarToMavenLocal = tasks.register<PublishJarToMavenLocalTask>("publishJarToMavenLocal") {
    version = serverJarVersion
}

tasks.register("installServerJar") {
    // Not sure if this is the best way to do it but it works...
    downloadMinecraftJar.get().greet()
    downloadMappings.get().greet()
    downloadEnigma.get().greet()
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

open class RunEnigmaTask : DefaultTask() {

    @Internal var version: String? = null
    @Internal var enigma: String? = null

    @TaskAction
    fun greet() {
        // Convert mappings to Enigma format (https://github.com/FabricMC/Enigma/issues/273)
        println("Converting server mappings to enigma format...")
        project.exec {
            commandLine("java", "-cp", "enigma-cli-${enigma}-all.jar", "cuchaz.enigma.command.Main", "convert-mappings", "proguard", "${version}-mappings.txt", "enigma_file", "${version}-server.mapping")
        }
        println("Mappings conversion complete!")

        // Ok, now lets map the jar!
        println("Deobfuscating server jar...")
        project.exec {
            commandLine("java", "-cp", "enigma-cli-${enigma}-all.jar", "cuchaz.enigma.command.Main", "deobfuscate", "${version}-server.jar", "${version}-server-deobfuscated.jar", "${version}-server.mapping")
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
    @Internal var enigma: TaskProvider<*>? = null
    @Internal var deobfuscateJar: TaskProvider<*>? = null
    @Internal var publish: TaskProvider<*>? = null

    @TaskAction
    fun greet() {
        dependsOn(minecraftJar)
        dependsOn(mappings)
        dependsOn(enigma)
        dependsOn(deobfuscateJar)
        dependsOn(publish)
    }
}