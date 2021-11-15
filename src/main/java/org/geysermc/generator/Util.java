package org.geysermc.generator;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.PrintStream;

public class Util {

    public static void initialize() {
        PrintStream err = System.err;
        PrintStream out = System.out;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        // Revert this stupid thing that the Bootstrap process does
        System.setErr(err);
        System.setOut(out);
    }
}
