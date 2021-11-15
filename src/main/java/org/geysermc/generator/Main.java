package org.geysermc.generator;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.PrintStream;

public class Main {

    public static void main(String[] args) {
        Util.initialize();

        MappingsGenerator generator = new MappingsGenerator();
        generator.generateItems();
        generator.generateBlocks();
        generator.generateSounds();
        generator.generateBiomes();
        generator.generateMapColors();
        generator.generateEnchantments();
    }
}
