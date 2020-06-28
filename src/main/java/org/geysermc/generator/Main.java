package org.geysermc.generator;

public class Main {

    public static void main(String[] args) {
        MappingsGenerator resourceGenerator = new MappingsGenerator();
        resourceGenerator.generateItems();
        resourceGenerator.generateBlocks();
    }
}
