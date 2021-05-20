package org.geysermc.generator;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;

import java.io.PrintStream;

public class Main {

    public static void main(String[] args) {
        PrintStream err = System.err;
        PrintStream out = System.out;
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        // Revert this stupid thing that the Bootstrap process does
        System.setErr(err);
        System.setOut(out);

        MappingsGenerator generator = new MappingsGenerator();
        generator.generateItems();
        generator.generateBlocks();
        generator.generateSounds();
    }
}
