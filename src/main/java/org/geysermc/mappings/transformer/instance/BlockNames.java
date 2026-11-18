package org.geysermc.mappings.transformer.instance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;

public final class BlockNames {
    public static final Transformer<BlockState, String> INSTANCE = Transformer.strong(builder -> builder
            .mapInput(BlockState::getBlock)

            // These don't exist on Bedrock
            .transform(Blocks.TEST_BLOCK, "unknown")
            .transform(Blocks.TEST_INSTANCE_BLOCK, "unknown")
            .transform(Blocks.VOID_AIR, "air")
            .transform(Blocks.CAVE_AIR, "air")

            // Named differently
            .transform(Blocks.POWERED_RAIL, "golden_rail")
            .transform(Blocks.DIRT_PATH, "grass_path")
            .transform(Blocks.SMALL_DRIPLEAF, "small_dripleaf_block")
            .transform(Blocks.BIG_DRIPLEAF_STEM, "big_dripleaf")
            .transform(Blocks.FLOWERING_AZALEA_LEAVES, "azalea_leaves_flowered")
            .transform(Blocks.ROOTED_DIRT, "dirt_with_roots")
            .transform(Blocks.POWDER_SNOW_CAULDRON, "cauldron")
            .transform(Blocks.WATER_CAULDRON, "cauldron")
            .transform(Blocks.LAVA_CAULDRON, "cauldron")
            .transform(Blocks.COPPER_BLOCK.waxed().unaffected(), "waxed_copper")
            .transform(Blocks.TRIPWIRE, "trip_wire")
            .transform(Blocks.MOVING_PISTON, "moving_block")
            .transform(Blocks.NOTE_BLOCK, "noteblock")
            .transform(Blocks.GLAZED_TERRACOTTA.lightGray(), "silver_glazed_terracotta")
            .transform(Blocks.COBWEB, "web")
            .transform(Blocks.DEAD_BUSH, "deadbush")
            .transform(Blocks.TALL_SEAGRASS, "seagrass")
            .transform(Blocks.BRICKS, "brick_block")
            .transform(Blocks.WALL_TORCH, "torch")
            .transform(Blocks.SOUL_WALL_TORCH, "soul_torch")
            .transform(Blocks.COPPER_WALL_TORCH, "copper_torch")
            .transform(Blocks.SPAWNER, "mob_spawner")
            .transform(Blocks.SNOW_BLOCK, "snow")
            .transform(Blocks.SNOW, "snow_layer")
            .transform(Blocks.SUGAR_CANE, "reeds")
            .transform(Blocks.NETHER_PORTAL, "portal")
            .transform(Blocks.JACK_O_LANTERN, "lit_pumpkin")
            .transform(Blocks.MELON, "melon_block")
            .transform(Blocks.ATTACHED_PUMPKIN_STEM, "pumpkin_stem")
            .transform(Blocks.ATTACHED_MELON_STEM, "melon_stem")
            .transform(Blocks.LILY_PAD, "waterlily")
            .transform(Blocks.TERRACOTTA, "hardened_clay")
            .transform(Blocks.DARK_OAK_SIGN, "darkoak_standing_sign")
            .transform(Blocks.DARK_OAK_WALL_SIGN, "darkoak_wall_sign")
            .transform(Blocks.COBBLESTONE_STAIRS, "stone_stairs")
            .transform(Blocks.STONE_STAIRS, "normal_stone_stairs")
            .transform(Blocks.NETHER_BRICKS, "nether_brick")
            .transform(Blocks.NETHER_QUARTZ_ORE, "quartz_ore")
            .transform(Blocks.SLIME_BLOCK, "slime")
            .transform(Blocks.PRISMARINE_BRICK_STAIRS, "prismarine_bricks_stairs")
            .transform(Blocks.END_STONE_BRICKS, "end_bricks")
            .transform(Blocks.END_STONE_BRICK_STAIRS, "end_brick_stairs")
            .transform(Blocks.BEETROOTS, "beetroot")
            .transform(Blocks.MAGMA_BLOCK, "magma")
            .transform(Blocks.RED_NETHER_BRICKS, "red_nether_brick")
            .transform(Blocks.SHULKER_BOX, "undyed_shulker_box")
            .transform(Blocks.KELP_PLANT, "kelp")
            .transform(Blocks.FROGSPAWN, "frog_spawn")
            .transform(Blocks.STONECUTTER, "stonecutter_block")
            .transform(Blocks.WEEPING_VINES_PLANT, "weeping_vines")
            .transform(Blocks.TWISTING_VINES_PLANT, "twisting_vines")

            // oak -> wooden / no prefix
            .transform(Blocks.OAK_SIGN, "standing_sign")
            .transform(Blocks.OAK_WALL_SIGN, "wall_sign")
            .transform(Blocks.OAK_TRAPDOOR, "trapdoor")
            .transform(Blocks.OAK_FENCE_GATE, "fence_gate")
            .transform(Blocks.OAK_DOOR, "wooden_door")
            .transform(Blocks.OAK_PRESSURE_PLATE, "wooden_pressure_plate")
            .transform(Blocks.OAK_BUTTON, "wooden_button")

            // State-dependent overrides
            .transform(Blocks.STONE_SLAB, state -> {
                if (state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
                    return "normal_stone_double_slab";
                }
                return "normal_stone_slab";
            })
            .transform(Blocks.DEEPSLATE_REDSTONE_ORE, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "lit_deepslate_redstone_ore";
                }
                return "deepslate_redstone_ore";
            })
            .transform(Blocks.REDSTONE_ORE, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "lit_redstone_ore";
                }
                return "redstone_ore";
            })
            .transform(Blocks.REDSTONE_TORCH, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "redstone_torch";
                }
                return "unlit_redstone_torch";
            })
            .transform(Blocks.REDSTONE_LAMP, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "lit_redstone_lamp";
                }
                return "redstone_lamp";
            })
            .transform(Blocks.REDSTONE_WALL_TORCH, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "redstone_torch";
                }
                return "unlit_redstone_torch";
            })
            .transform(Blocks.PISTON_HEAD, state -> {
                if (state.getValue(BlockStateProperties.PISTON_TYPE) == PistonType.STICKY) {
                    return "sticky_piston_arm_collision";
                } else {
                    return "piston_arm_collision";
                }
            })
            .transform(Blocks.REPEATER, state -> {
                if (state.getValue(BlockStateProperties.POWERED)) {
                    return "powered_repeater";
                } else {
                    return "unpowered_repeater";
                }
            })
            .transform(Blocks.COMPARATOR, state -> {
                if (state.getValue(BlockStateProperties.POWERED)) {
                    return "powered_comparator";
                } else {
                    return "unpowered_comparator";
                }
            })
            .transform(Blocks.DAYLIGHT_DETECTOR, state -> {
                if (state.getValue(BlockStateProperties.INVERTED)) {
                    return "daylight_detector_inverted";
                } else {
                    return "daylight_detector";
                }
            })
            .transform(Blocks.LIGHT, state -> {
                int lightValue = state.getValue(LightBlock.LEVEL);
                return "light_block_" + lightValue;
            })
            .transform(Blocks.CAVE_VINES_PLANT, state -> {
                if (state.getValue(CaveVinesPlantBlock.BERRIES)) {
                    return "cave_vines_body_with_berries";
                } else {
                    return "cave_vines";
                }
            })
            .transform(Blocks.CAVE_VINES, state -> {
                if (state.getValue(CaveVinesPlantBlock.BERRIES)) {
                    return "cave_vines_head_with_berries";
                } else {
                    return "cave_vines";
                }
            })
            .transform(Blocks.WATER, state -> {
                if (state.getValue(LiquidBlock.LEVEL) == 0) {
                    return "water";
                } else {
                    return "flowing_water";
                }
            })
            .transform(Blocks.LAVA, state -> {
                if (state.getValue(LiquidBlock.LEVEL) == 0) {
                    return "lava";
                } else {
                    return "flowing_lava";
                }
            })
            .transform(Blocks.FURNACE, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "lit_furnace";
                } else {
                    return "furnace";
                }
            })
            .transform(Blocks.SMOKER, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "lit_smoker";
                } else {
                    return "smoker";
                }
            })
            .transform(Blocks.BLAST_FURNACE, state -> {
                if (state.getValue(BlockStateProperties.LIT)) {
                    return "lit_blast_furnace";
                } else {
                    return "blast_furnace";
                }
            })

            // Class-dependent names
            .instanceOf(BedBlock.class, "bed")
            .instanceOf(FlowerPotBlock.class, "flower_pot")
            .instanceOfStrong(AbstractBannerBlock.class, sub -> sub
                    .instanceOf(WallBannerBlock.class, "wall_banner")
                    .finish("standing_banner"))

            .instanceOf(StandingSignBlock.class, Transformers.registryKey(BuiltInRegistries.BLOCK).chain(Transformers.replace("sign", "standing_sign")))
            .instanceOf(WallHangingSignBlock.class, Transformers.registryKey(BuiltInRegistries.BLOCK).chain(Transformers.replace("wall_", "")))
            .instanceOf(WallSkullBlock.class, Transformers.registryKey(BuiltInRegistries.BLOCK).chain(Transformers.replace("wall_", "")))

            .root()

            // An edge case
            .transformBuilder(state -> state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE, sub -> sub
                    .mapInput(BlockState::getBlock)
                    .mark()
                    .mapInput(Transformers.registryKey(BuiltInRegistries.BLOCK))
                    .transform(Transformers.contains("cut_copper"), Transformers.replace("cut", "double_cut"))
                    .reset()
                    .finish(Transformers.registryKey(BuiltInRegistries.BLOCK).chain(Transformers.replace("slab", "double_slab"))))

            .mapInput(BlockState::getBlock)
            .finish(Transformers.registryKey(BuiltInRegistries.BLOCK)));
    
    private BlockNames() {}
}
