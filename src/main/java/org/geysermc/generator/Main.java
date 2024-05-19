package org.geysermc.generator;

public class Main {

    public static void main(String[] args) {
        Util.initialize();

        MappingsGenerator generator = new MappingsGenerator();
        generator.generateItems();
        BlockGenerator.generate();
        CollisionGenerator.generate();
        generator.generateSounds();
        generator.generateBiomes();
        generator.generateMapColors();
        generator.generateEnchantments();
        generator.generateParticles();
        //generator.generateInteractionData();
    }
}
