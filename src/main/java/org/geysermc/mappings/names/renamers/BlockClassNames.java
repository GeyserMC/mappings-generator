package org.geysermc.mappings.names.renamers;

import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import org.geysermc.mappings.names.TypeRenamer;

import java.util.List;
import java.util.Optional;

public final class BlockClassNames {
    private static final List<Class<? extends Block>> MIRRORED_CLASSES = List.of(BedBlock.class, CauldronBlock.class, ChestBlock.class, DoorBlock.class,
            FlowerPotBlock.class, FurnaceBlock.class, LecternBlock.class, MovingPistonBlock.class,
            ShulkerBoxBlock.class, TrapDoorBlock.class, WallSkullBlock.class, ButtonBlock.class);
    public static final TypeRenamer<Block, String> INSTANCE = TypeRenamer.of(BlockClassNames::getClassName, builder -> builder
            .rename(Blocks.PISTON, "PistonBlock")
            .rename(Blocks.STICKY_PISTON, "PistonBlock"));

    private BlockClassNames() {}

    private static String getClassName(Block block) {
        return switch (block) {
            case AbstractBannerBlock _ -> "BannerBlock";
            case SkullBlock _ -> "SkullBlock";
            case AbstractCauldronBlock _ -> "CauldronBlock";
            default -> getMirroredClassName(block).orElse("Block");
        };
    }

    private static Optional<String> getMirroredClassName(Block block) {
        for (Class<? extends Block> mirroredClass : MIRRORED_CLASSES) {
            if (mirroredClass.isAssignableFrom(block.getClass())) {
                return Optional.of(mirroredClass.getSimpleName());
            }
        }
        return Optional.empty();
    }
}
