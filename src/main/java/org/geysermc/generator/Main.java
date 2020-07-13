package org.geysermc.generator;

public class Main {

    public static void main(String[] args) {
        MappingsGenerator generator = new MappingsGenerator();
        generator.generateItems();
        generator.generateBlocks();
        generator.generateSounds();
        generator.generateMapColors();
    }
}
