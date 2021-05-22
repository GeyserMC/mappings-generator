package org.geysermc.generator;

import net.minecraft.server.Bootstrap;

public class Main {

    public static void main(String[] args) {
        Bootstrap.bootStrap();

        MappingsGenerator generator = new MappingsGenerator();
        generator.generateItems();
        generator.generateBlocks();
        generator.generateSounds();
    }
}
