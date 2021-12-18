# Geyser Mapping Generator

A standalone program that generates (most of) the [mappings](https://github.com/GeyserMC/mappings) used throughout Geyser.

## Setup

- Clone this repository locally: `git clone https://github.com/GeyserMC/mappings-generator`
- Ensure submodules are cloned
- Navigate to the `mappings-generator` directory.

## Running

Use the `Run` button in your IDE.
Once the program is done running, files will be created containing mappings needed for the version you are using. Please keep in mind that while this generator will map most of the needed information on its own, in many instances (such as with game updates with completely new values), you will have to do some manual mapping of some kind or create mappers within this project.

## Updating for future versions

Update the `javaMinecraftVersion` variable in `build.gradle.kts` to your desired version.
