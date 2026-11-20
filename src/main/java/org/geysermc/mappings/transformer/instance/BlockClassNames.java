package org.geysermc.mappings.transformer.instance;

import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
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
import org.geysermc.mappings.transformer.Transformer;

import java.util.List;
import java.util.Optional;

public final class BlockClassNames {
    private static final List<Class<? extends Block>> MIRRORED_CLASSES = List.of(BedBlock.class, ChestBlock.class, DoorBlock.class,
            FlowerPotBlock.class, FurnaceBlock.class, LecternBlock.class, MovingPistonBlock.class,
            ShulkerBoxBlock.class, TrapDoorBlock.class, SkullBlock.class, WallSkullBlock.class, ButtonBlock.class);

    public static final Transformer<Block, String> INSTANCE = Transformer.of(builder -> builder
            .transform(Blocks.PISTON, "PistonBlock")
            .transform(Blocks.STICKY_PISTON, "PistonBlock")
            .instanceOf(AbstractBannerBlock.class, "BannerBlock")
            .instanceOf(SkullBlock.class, "SkullBlock")
            .instanceOf(AbstractCauldronBlock.class, "CauldronBlock")

            .maybeTransform(BlockClassNames::getMirroredClassName)
            .finish("Block"));

    private BlockClassNames() {}

    private static Optional<String> getMirroredClassName(Block block) {
        for (Class<? extends Block> mirroredClass : MIRRORED_CLASSES) {
            if (mirroredClass.isAssignableFrom(block.getClass())) {
                return Optional.of(mirroredClass.getSimpleName());
            }
        }
        return Optional.empty();
    }
}
