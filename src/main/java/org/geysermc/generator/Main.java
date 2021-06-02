package org.geysermc.generator;

import net.minecraft.SharedConstants;

import net.minecraft.core.Registry;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.gameevent.PositionSourceType;

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
    }
}
