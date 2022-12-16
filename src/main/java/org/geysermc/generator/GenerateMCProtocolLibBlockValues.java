package org.geysermc.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class GenerateMCProtocolLibBlockValues {

    public static void main(String[] args) {
        Util.initialize();

        format("NOTE_BLOCK", Blocks.NOTE_BLOCK);
        format("STICKY_PISTON", Blocks.STICKY_PISTON);
        format("PISTON", Blocks.PISTON);
        format("MOB_SPAWNER", Blocks.SPAWNER);
        format("CHEST", Blocks.CHEST);
        format("ENDER_CHEST", Blocks.ENDER_CHEST);
        format("TRAPPED_CHEST", Blocks.TRAPPED_CHEST);
        format("END_GATEWAY", Blocks.END_GATEWAY);
        format("SHULKER_BOX_LOWER", Blocks.SHULKER_BOX);
        format("SHULKER_BOX_HIGHER", Blocks.BLACK_SHULKER_BOX);
    }

    private static void format(String name, Block block) {
        System.out.println("private static final int " + name + " = " + BuiltInRegistries.BLOCK.getId(block) + ";");
    }
}
