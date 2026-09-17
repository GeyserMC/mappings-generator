# Geyser Mappings Generator

[![License: MIT](https://img.shields.io/github/license/GeyserMC/mappings-generator)](https://github.com/GeyserMC/mappings-generator/blob/master/LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)

A standalone program that generates (most of) the [`mappings`](https://github.com/GeyserMC/mappings) used throughout Geyser, and exports various other data from Minecraft Java as well, used in both Geyser and MCPL.

The generator makes use of Minecraft Java's source code and generates its data through Fabric's [data generation API](https://docs.fabricmc.net/develop/data-generation/setup). The generated files are published to [GitHub releases](https://github.com/GeyserMC/mappings-generator/releases),
licensed under the MIT license [^1].

## Setup

- Clone this repository locally: `git clone https://github.com/GeyserMC/mappings-generator`.
- Navigate to the `mappings-generator` directory.
- Ensure the `mappings/mappings` submodule is cloned: `git submodule update --init --recursive`.
    - If you don't do this, the generator will try to do it for you upon running, if you have `git` installed properly.

## Running

Run the `runDatagen` Gradle task. This will start the data generator, which will:

- Try to clone the `mappings/mappings` submodule, if it was not cloned already.
- Download [`bedrock-data.zip`](https://github.com/CloudburstMC/Data) and [`bedrock-samples.zip`](https://github.com/Mojang/bedrock-samples) locally, which contain data required to run some generators.
- Run all generators. At the end of the generation process, a list of added, changed, and deleted files will be presented to you.

Alternatively, you can run one of the following Gradle tasks to only run a subset of the generators:

- `runMCPL` to only run the generators generating data for MCPL.
- `runJavaclass` to only run the generators generating classes for Geyser.
- `runMappings` to only run the generators generating data for the [`mappings`](https://github.com/GeyserMC/mappings) repository.

Once the generator is done, files will have been created containing data needed for the Minecraft Java and Bedrock versions you are using.
Please keep in mind that while the generator will map most of the needed information on its own, in some instances (especially with particles and sounds),
you will have to do manual mapping of some kind or create mappers within this project.

The generator will alert you when manual mapping is necessary: take note of error logs in the generator's output, which are highlighted in red, and indicate incomplete mappings. Warnings, highlighted in yellow, are also not to be ignored.

## Versioning

The output of the generator is published to [GitHub releases](https://github.com/GeyserMC/mappings-generator/releases). The generator, and the releases it publishes, follow [Semantic Versioning](https://semver.org/). Generally:

- The major version segment is incremented for breaking changes to the generated output (field renames/removals, etc.).
- The minor version segment is incremented for non-breaking changes/additions to the generated output (new mappings, fields, etc.).
- The patch version segment is incremented for non-breaking updates to the generated output (updates to newer Minecraft versions, mapping improvements, etc.).

## Changing the Minecraft versions

All versions, not just those of libraries, are kept in the [`libs.versions.toml`](gradle/libs.versions.toml) file. Be sure to keep the libraries used up-to-date.

When updating the generator, make sure to increment the version inline with our versioning guidelines.

### Updating the Minecraft Java version

Update the `minecraft-java` field in the version catalogue, and make sure `fabric-loom` and `fabric-loader` are up-to-date. Set `fabric-api` to the latest version for the respective Minecraft version.

Then, attempt to run the generator. Resolve all compilation errors (if there are any), and be sure to take note of error logs indicating incomplete mappings.

### Updating the Minecraft Bedrock version

Update the `minecraft-bedrock-tag` and `minecraft-bedrock-data` fields in the version catalogue. The former should be set to the appropriate tag at [Mojang/bedrock-samples](https://github.com/Mojang/bedrock-samples/releases),
and the latter should be set to the respective commit hash for that version at [CloudburtsMC/Data](https://github.com/CloudburstMC/Data). Make sure that the `cloudburst-protocol` version is up-to-date as well, and supports
the Bedrock version you're targeting.

Then, run the generator. If all goes well, the mappings should now reflect the Bedrock version you targeted.

## Writing new generators

The generator is set up to take a lot of the IO work for you, and as such you generally won't need to write code for reading or writing files yourself. You do need to write [codecs](https://docs.fabricmc.net/develop/serialization/codecs)
for parsing and serialising the data you want to read or generate, however, if this scares you, you may also use the [`MappingsCodecs#JSON_ELEMENT`](src/main/java/org/geysermc/mappings/util/MappingsCodecs.java)
and `CompoundTag#CODEC` codecs to read and write to `JsonElement`s or `CompoundTag`s directly.

Before writing a generator, consider what data you want to generate/export. If the data is small in quantity and exported directly from Minecraft Java's source code, consider adding it to the [`util.json`](mappings/mappings/util.json) mappings,
instead of writing a whole new generator. Adding new data to the `util.json` mappings generally doesn't take much effort: you only need to add your data to the codec in [`UtilMappings.java`](src/main/java/org/geysermc/mappings/definitions/util/UtilMappings.java),
and extract it in the `UtilMappings#create` method.

If you do need to write a new generator, you need to take the following steps:

1. Write one or multiple definitions for the kind of data you want to read- and write in the [`org.geysermc.mappings.definitions`](src/main/java/org/geysermc/mappings/definitions) package. Generally, these are `record`s with codecs.
2. Create one or more [`FileType`](src/main/java/org/geysermc/mappings/FileType.java)s for the data you want to read- and write. You'll need to add at least one type for the new file you're generating. You also need to add new types for each file you want to read, if they don't exist already. This includes files in [CloudburstMC/data](https://github.com/CloudburstMC/Data) or [Mojang/bedrock-samples](https://github.com/Mojang/bedrock-samples).
3. Create a generator in the [`org.geysermc.mappings.generator`](src/main/java/org/geysermc/mappings/generator) package. The generator class must be `final` and must extend [`MappingsGenerator`](src/main/java/org/geysermc/mappings/generator/MappingsGenerator.java).
    - Consider sharing the generation code with the definitions you made earlier, so that the generator class doesn't grow too big.
    - If you need to write a map of renames, consider creating a "renamer" in the [`org.geysermc.mappings.names`](src/main/java/org/geysermc/mappings/names) package. See [`Renamers`](src/main/java/org/geysermc/mappings/names/Renamers.java).
    - You may need a `RegistryAccess` instance, and you may need to read data from [CloudburstMC/data](https://github.com/CloudburstMC/Data) or [Mojang/bedrock-samples](https://github.com/Mojang/bedrock-samples). The Javadocs in [`MappingsGenerator`](src/main/java/org/geysermc/mappings/generator/MappingsGenerator.java) describe how to accomplish this.
4. Finally, add your generator to a generator subset in [`MappingsGenerators`](src/main/java/org/geysermc/mappings/MappingsGenerators.java), and run it!

A few notes:

- The core classes used in the generator's code have Javadocs. Make sure to read these as they can help you quite a bit!
- If you're not sure how to accomplish something, looking at the code of other generators might help you out.
- If you're simply aiming to write a new `*Tag.java` class, containing all vanilla tags of a registry, take a look at [`TagListGenerator#addProviders`](src/main/java/org/geysermc/mappings/generator/javaclass/TagListGenerator.java).
- If you're stuck somewhere, don't be afraid to reach out on [our Discord](https://discord.gg/geysermc)!
- Make sure to log information! Use error logs **only** if an error occurred that causes the generated file(s) to be incomplete. Generally, you can obtain a logger for a class as follows:
    ```java
    private static final Logger LOGGER = LogUtils.getLogger();
    ```
    The `LogUtils` class is provided by Mojang.
- Make sure to increment the version inline with our versioning guidelines.
- Be sure to include the new data in the README section below.

## Data generated

The generator generates the following data:

- Various stub classes used in MCPL:
    - `BuiltinSound.java`, an enum containing all of Minecraft Java's built-in sound events.
    - `ClientboundBlockEventPacket.java`, a list of constants used for identifying Minecraft Java's block events.
    - `CustomStatistic.java`, an enum containing all of Minecraft Java's custom statistics.
    - `LevelEventType.java`, an enum containing all of Minecraft Java's level events.
- Data-driven registry data used in MCPL:
    - `networkCodec.nbt`, containing registry data for all of Minecraft Java's network-synced registries.
    - `networkTags.nbt`, containing vanilla registry tags for all of Minecraft Java's registries.
- Various stub classes used in Geyser:
    - `Blocks.java`: listing all of Minecraft Java's blocks.
    - `GameRules.java`: listing all of Minecraft Java's game rules.
    - `Items.java`: listing all of Minecraft Java's items.
    - `MapColor.java`: listing all of Minecraft Java's map colours.
    - `Properties.java`: listing all of Minecraft Java's block state properties.
    - `BlockTag.java`: listing all of Minecraft Java's vanilla block tags.
    - `DialogTag.java`: listing all of Minecraft Java's vanilla dialog tags.
    - `EnchantmentTag.java`: listing all of Minecraft Java's vanilla enchantment tags.
    - `ItemTag.java`: listing all of Minecraft Java's vanilla item tags.
- The mappings present in our [`mappings`](https://github.com/GeyserMC/mappings) repository:
    - `biomes.json`: a map from (vanilla) Minecraft Java biome to its respective Bedrock network ID.
    - `blocks.nbt` (and `blocks_debug.json`): a map from Minecraft Java block state to its respective Bedrock block state.
    - `block_shapes.nbt`: describing the shapes of all of Minecraft Java's block states.
    - `collisions.nbt`: describing the shapes of all of Minecraft Java's block state collision boxes.
    - `decorated_pot_patterns.json`: a map from (vanilla) Minecraft Java decorated pot pattern (asset ID) to its respective Bedrock item ID.
    - `interactions.json`: describing how interactions with a Minecraft Java block state may be processed.
    - `item_components.nbt`: Minecraft Bedrock item components, patched by the generator to possibly allow extra items to be put in the off-hand.
    - `additional_offhand_items.json`: an array of all Bedrock items that were patched by the generator to allow putting into the off-hand.
    - `item_data_components.json`: listing default data components for all of Minecraft Java's items, encoded to base 64 using Mojang's stream codecs.
    - `items.json`: a map from Minecraft Java item to its respective Bedrock item (with some additional data).
    - `particles.json`: a map from Minecraft Java particle to its respective Bedrock identifier or event type.
    - `resolvable_item_data_components.json`: listing default data components for all of Minecraft Java's items, for components that hold data-driven content.
    - `sounds.json`: a map from Minecraft Java sound to its respective Bedrock identifier or event type.
    - `util.json`: holding various small data exported from Minecraft Java.

[^1]: Please note that we only license the original work contributors have the right to license. We do not relicense Minecraft, its data, or any other material owned by Mojang or Microsoft.
