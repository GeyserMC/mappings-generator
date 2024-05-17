package org.geysermc.generator;

import org.geysermc.generator.state.CollisionGenerator;

public class Main {

    public static void main(String[] args) {
        Util.initialize();

        MappingsGenerator generator = new MappingsGenerator();
        generator.generateItems();
        generator.generateBlocks();
        CollisionGenerator.generate();
        generator.generateSounds();
        generator.generateBiomes();
        generator.generateMapColors();
        generator.generateEnchantments();
        generator.generateParticles();
        generator.generateInteractionData();
    }
}
