package org.geysermc.resources;

public class Main {

    public static void main(String[] args) {
        ResourceGenerator resourceGenerator = new ResourceGenerator();
        resourceGenerator.generateItems();
        resourceGenerator.generateBlocks();
    }
}
