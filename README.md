# Geyser Mapping Generator

A standalone program that generates (most of) the [mappings](https://github.com/GeyserMC/mappings) used throughout Geyser.

## Setup

- Clone this repository locally: `git clone https://github.com/GeyserMC/mappings-generator`
- Navigate to the `mappings-generator` directory.
- Download the specified Minecraft server from the Minecraft website.
- Run the following:

```bash
mvn org.apache.maven.plugins:maven-install-plugin:install-file -Dfile=server.jar -DgroupId="net.minecraft" -DartifactId="server" -Dversion="(version)-SNAPSHOT" -Dpackaging="jar"
```
Replace `(version)` with the Minecraft server version. If `server.jar` isn't in the current directory, set `-DFile` to point to the correct location.

- Clone the [Yarn](https://github.com/FabricMC/yarn) repository using the same process above.
- Ensure the current Yarn git branch is set to the desired Minecraft version.
- In the directory run `./gradlew mapNamedJar` (Linux, Git console and PowerShell) / `gradlew mapNamedJar` (Windows CMD)
- Copy the `VERSION-named.jar` to the `mappings-generator` folder.
- Replacing `(version)` with your desired Minecraft version, run:

```bash
mvn org.apache.maven.plugins:maven-install-plugin:install-file -Dfile=(version)-named.jar -DgroupId="net.fabricmc" -DartifactId="(version)-named" -Dversion="(version)-SNAPSHOT" -Dpackaging="jar"
``` 

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

If you want mappings for later versions, change the versions for the Fabric mappings and Minecraft server in the `pom.xml` and update the jar files with the process above.
