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
        generator.generateParticles();
        //generator.generateInteractionData();
        RecipeGenerator.generate();
        DataComponentGenerator.generate();
        UtilGenerator.generate();
    }
}
