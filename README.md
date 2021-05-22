# Geyser Mapping Generator

A standalone program that generates (most of) the [mappings](https://github.com/GeyserMC/mappings) used throughout Geyser.

## Setup

- Clone this repository locally: `git clone https://github.com/GeyserMC/mappings-generator`
- Navigate to the `mappings-generator` directory.
- Run `gradlew installServerJar`. This will download the needed resources, such as the Minecraft server jar along with tooling such as enigma to deobfuscate it so it can be used with this project.
- If in an IDE such as IntelliJ, refresh your dependencies - this will then add the generated server jar to your dependencies.

- Run `./gradlew build` / `gradlew build` to generate some required Yarn constants
- Copy the `build/libs/yarn-VERSION+build.local-constants.jar` file to the `mappings-generator` folder.
- Replacing `(version)` with your desired Minecraft version, run:

```bash
mvn org.apache.maven.plugins:maven-install-plugin:install-file -Dfile=yarn-(version)+build.local-constants.jar -DgroupId="net.fabricmc" -DartifactId="yarn" -Dversion="(version)+build.local-constants" -Dpackaging="jar"
```

## Running

Use the `Run` button in your IDE.
Once the program is done running, files will be created containing mappings needed for the version you are using. Please keep in mind that while this generator will map most of the needed information manually, in many instances (such as with game updates with completely new values), you will have to do some manual mapping of some kind or create mappers within this project.

## Updating for future versions

Updating for future versions is fairly simple. In `build.gradle.kts`, there are 3 primary values you will want to change:
```kotlin
val serverJarHash = "054b2065dd63c3e4227879046beae7acaeb7e8d3"
val serverMappingsHash = "8d6960e996a40b8350f5973d8a237469a9a6a7bc"
val serverJarVersion = "21w20a"
```
These control which server jar to download, mappings file, and the version you will be downloading. These hashes can be retrieved from the relevant Minecraft wiki page for which version you want to use. These can be found in the relevant download links on the wiki page.

Here is how it would look for Minecraft 1.16.5:
```kotlin
val serverJarHash = "1b557e7b033b583cd9f66746b7a9ab1ec1673ced"
val serverMappingsHash = "41285beda6d251d190f2bf33beadd4fee187df7a"
val serverJarVersion = "1.16.5"
```

Once you have updated these values, once again run the `gradlew installServerJar` command to install this to your local maven repository, then refresh your dependencies in order to work with the updated code.

Keep in mind that this code is not guaranteed to work across versions as it relies on Minecraft code which can change at any time.