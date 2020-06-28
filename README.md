# Geyser Mapping Generator

A standalone program that generates (most of) the [mappings](https://github.com/GeyserMC/mappings) used throughout Geyser.

If you would like to use this to generate mappings, clone this repository locally and use the `Run` button in your IDE.
Once the program is done running, files will be created containing mappings needed for the version you are using. Please keep in mind that while this generator will map most of the needed information manually, in many instances (such as with game updates with completely new values), you will have to do some manual mapping of some kind or create mappers within this project.

NOTE: This program uses the Fabric mappings, and if you want mappings for later versions, simply update the jar(s) in the `pom.xml`. In most scenarios, this should remain the same across versions, but some updating may need to be done when it comes to larger releases.
