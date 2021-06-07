package org.geysermc.generator;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.PrintStream;

public class Main {

    public static void main(String[] args) {
        PrintStream err = System.err;
        PrintStream out = System.out;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        // Revert this stupid thing that the Bootstrap process does
        System.setErr(err);
        System.setOut(out);

        MappingsGenerator generator = new MappingsGenerator();
        generator.generateItems();
        generator.generateBlocks();
        generator.generateSounds();

        // todo: the following items/blocks to be remapped properly once added to bedrock edition
        // bundle
        // candle, candle_cakes, potted_azalea,
    }
}
