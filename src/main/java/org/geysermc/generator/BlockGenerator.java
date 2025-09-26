package org.geysermc.generator;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.generator.state.BlockMapper;
import org.geysermc.generator.state.BlockMappers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class BlockGenerator {

    public static final Map<Block, String> NAME_OVERRIDES = new HashMap<>();
    public static final Map<Block, Function<BlockState, String>> STATE_BLOCK_OVERRIDES = new HashMap<>();

    static {
        Util.initialize();

        // These don't exist on Bedrock
        NAME_OVERRIDES.put(Blocks.TEST_BLOCK, "unknown");
        NAME_OVERRIDES.put(Blocks.TEST_INSTANCE_BLOCK, "unknown");
        NAME_OVERRIDES.put(Blocks.VOID_AIR, "air");
        NAME_OVERRIDES.put(Blocks.CAVE_AIR, "air");

        NAME_OVERRIDES.put(Blocks.POWERED_RAIL, "golden_rail");
        NAME_OVERRIDES.put(Blocks.DIRT_PATH, "grass_path");
        NAME_OVERRIDES.put(Blocks.SMALL_DRIPLEAF, "small_dripleaf_block");
        NAME_OVERRIDES.put(Blocks.BIG_DRIPLEAF_STEM, "big_dripleaf");
        NAME_OVERRIDES.put(Blocks.FLOWERING_AZALEA_LEAVES, "azalea_leaves_flowered");
        NAME_OVERRIDES.put(Blocks.ROOTED_DIRT, "dirt_with_roots");
        NAME_OVERRIDES.put(Blocks.POWDER_SNOW_CAULDRON, "cauldron");
        NAME_OVERRIDES.put(Blocks.WATER_CAULDRON, "cauldron");
        NAME_OVERRIDES.put(Blocks.LAVA_CAULDRON, "cauldron");
        NAME_OVERRIDES.put(Blocks.WAXED_COPPER_BLOCK, "waxed_copper");
        NAME_OVERRIDES.put(Blocks.TRIPWIRE, "trip_wire");
        NAME_OVERRIDES.put(Blocks.MOVING_PISTON, "moving_block");
        NAME_OVERRIDES.put(Blocks.NOTE_BLOCK, "noteblock");
        NAME_OVERRIDES.put(Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, "silver_glazed_terracotta");
        NAME_OVERRIDES.put(Blocks.COBWEB, "web");
        NAME_OVERRIDES.put(Blocks.DEAD_BUSH, "deadbush");
        NAME_OVERRIDES.put(Blocks.TALL_SEAGRASS, "seagrass");
        NAME_OVERRIDES.put(Blocks.BRICKS, "brick_block");
        NAME_OVERRIDES.put(Blocks.WALL_TORCH, "torch");
        NAME_OVERRIDES.put(Blocks.SOUL_WALL_TORCH, "soul_torch");
        NAME_OVERRIDES.put(Blocks.SPAWNER, "mob_spawner");
        NAME_OVERRIDES.put(Blocks.SNOW_BLOCK, "snow");
        NAME_OVERRIDES.put(Blocks.SNOW, "snow_layer");
        NAME_OVERRIDES.put(Blocks.SUGAR_CANE, "reeds");
        NAME_OVERRIDES.put(Blocks.NETHER_PORTAL, "portal");
        NAME_OVERRIDES.put(Blocks.JACK_O_LANTERN, "lit_pumpkin");
        NAME_OVERRIDES.put(Blocks.MELON, "melon_block");
        NAME_OVERRIDES.put(Blocks.ATTACHED_PUMPKIN_STEM, "pumpkin_stem");
        NAME_OVERRIDES.put(Blocks.ATTACHED_MELON_STEM, "melon_stem");
        NAME_OVERRIDES.put(Blocks.LILY_PAD, "waterlily");
        NAME_OVERRIDES.put(Blocks.TERRACOTTA, "hardened_clay");
        NAME_OVERRIDES.put(Blocks.DARK_OAK_SIGN, "darkoak_standing_sign");
        NAME_OVERRIDES.put(Blocks.DARK_OAK_WALL_SIGN, "darkoak_wall_sign");
        NAME_OVERRIDES.put(Blocks.COBBLESTONE_STAIRS, "stone_stairs");
        NAME_OVERRIDES.put(Blocks.STONE_STAIRS, "normal_stone_stairs");
        NAME_OVERRIDES.put(Blocks.NETHER_BRICKS, "nether_brick");
        NAME_OVERRIDES.put(Blocks.NETHER_QUARTZ_ORE, "quartz_ore");
        NAME_OVERRIDES.put(Blocks.SLIME_BLOCK, "slime");
        NAME_OVERRIDES.put(Blocks.PRISMARINE_BRICK_STAIRS, "prismarine_bricks_stairs");
        NAME_OVERRIDES.put(Blocks.END_STONE_BRICKS, "end_bricks");
        NAME_OVERRIDES.put(Blocks.END_STONE_BRICK_STAIRS, "end_brick_stairs");
        NAME_OVERRIDES.put(Blocks.BEETROOTS, "beetroot");
        NAME_OVERRIDES.put(Blocks.MAGMA_BLOCK, "magma");
        NAME_OVERRIDES.put(Blocks.RED_NETHER_BRICKS, "red_nether_brick");
        NAME_OVERRIDES.put(Blocks.SHULKER_BOX, "undyed_shulker_box");
        NAME_OVERRIDES.put(Blocks.KELP_PLANT, "kelp");
        NAME_OVERRIDES.put(Blocks.FROGSPAWN, "frog_spawn");
        NAME_OVERRIDES.put(Blocks.STONECUTTER, "stonecutter_block");
        NAME_OVERRIDES.put(Blocks.WEEPING_VINES_PLANT, "weeping_vines");
        NAME_OVERRIDES.put(Blocks.TWISTING_VINES_PLANT, "twisting_vines");

        // oak -> wooden / no prefix
        NAME_OVERRIDES.put(Blocks.OAK_SIGN, "standing_sign");
        NAME_OVERRIDES.put(Blocks.OAK_WALL_SIGN, "wall_sign");
        NAME_OVERRIDES.put(Blocks.OAK_TRAPDOOR, "trapdoor");
        NAME_OVERRIDES.put(Blocks.OAK_FENCE_GATE, "fence_gate");
        NAME_OVERRIDES.put(Blocks.OAK_DOOR, "wooden_door");
        NAME_OVERRIDES.put(Blocks.OAK_PRESSURE_PLATE, "wooden_pressure_plate");
        NAME_OVERRIDES.put(Blocks.OAK_BUTTON, "wooden_button");

        // TODO remove in 1.21.9
        NAME_OVERRIDES.put(Blocks.CHAIN, "iron_chain");

        STATE_BLOCK_OVERRIDES.put(Blocks.STONE_SLAB, state -> {
            if (state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
                return "normal_stone_double_slab";
            }
            return "normal_stone_slab";
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.DEEPSLATE_REDSTONE_ORE, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "lit_deepslate_redstone_ore";
            }
            return "deepslate_redstone_ore";
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.REDSTONE_ORE, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "lit_redstone_ore";
            }
            return "redstone_ore";
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.REDSTONE_TORCH, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "redstone_torch";
            }
            return "unlit_redstone_torch";
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.REDSTONE_LAMP, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "lit_redstone_lamp";
            }
            return "redstone_lamp";
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.REDSTONE_WALL_TORCH, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "redstone_torch";
            }
            return "unlit_redstone_torch";
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.PISTON_HEAD, state -> {
            if (state.getValue(BlockStateProperties.PISTON_TYPE) == PistonType.STICKY) {
                return "sticky_piston_arm_collision";
            } else {
                return "piston_arm_collision";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.REPEATER, state -> {
            if (state.getValue(BlockStateProperties.POWERED)) {
                return "powered_repeater";
            } else {
                return "unpowered_repeater";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.COMPARATOR, state -> {
            if (state.getValue(BlockStateProperties.POWERED)) {
                return "powered_comparator";
            } else {
                return "unpowered_comparator";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.DAYLIGHT_DETECTOR, state -> {
           if (state.getValue(BlockStateProperties.INVERTED)) {
               return "daylight_detector_inverted";
           } else {
               return "daylight_detector";
           }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.LIGHT, state -> {
            int lightValue = state.getValue(LightBlock.LEVEL);
            return "light_block_" + lightValue;
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.CAVE_VINES_PLANT, state -> {
           if (state.getValue(CaveVinesPlantBlock.BERRIES)) {
               return "cave_vines_body_with_berries";
           } else {
               return "cave_vines";
           }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.CAVE_VINES, state -> {
            if (state.getValue(CaveVinesPlantBlock.BERRIES)) {
                return "cave_vines_head_with_berries";
            } else {
                return "cave_vines";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.WATER, state -> {
            if (state.getValue(LiquidBlock.LEVEL) == 0) {
                return "water";
            } else {
                return "flowing_water";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.LAVA, state -> {
            if (state.getValue(LiquidBlock.LEVEL) == 0) {
                return "lava";
            } else {
                return "flowing_lava";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.FURNACE, state -> {
           if (state.getValue(BlockStateProperties.LIT)) {
               return "lit_furnace";
           } else {
               return "furnace";
           }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.SMOKER, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "lit_smoker";
            } else {
                return "smoker";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.BLAST_FURNACE, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "lit_blast_furnace";
            } else {
                return "blast_furnace";
            }
        });
    }

    public static void main(String[] args) {
        generate();
    }

    public static void generate() {
        BlockMappers.registerMappers();

        // Read block states that we know exist to confirm our mapping
        NbtList<NbtMap> palette;
        File blockPalette = new File("palettes/block_palette.nbt");
        if (!blockPalette.exists()) {
            System.out.println("Could not find block palette (block_palette.nbt), won't be able to confirm block mappings!");
            return;
        }

        try {
            InputStream stream = new FileInputStream(blockPalette);

            try (NBTInputStream nbtInputStream = new NBTInputStream(new DataInputStream(new GZIPInputStream(stream)))) {
                NbtMap ret = (NbtMap) nbtInputStream.readTag();
                palette = (NbtList<NbtMap>) ret.getList("blocks", NbtType.COMPOUND);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to get blocks from block palette", e);
        }

        Set<NbtMap> vanillaStates = new HashSet<>();
        for (NbtMap map : palette) {
            var builder = map.toBuilder();
            builder.remove("name_hash");
            builder.remove("version");
            builder.remove("block_id");
            builder.remove("network_id");
            vanillaStates.add(builder.build());
        }

        List<Pair<BlockState, BlockEntry>> newMappings = new ArrayList<>(Block.BLOCK_STATE_REGISTRY.size());

        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            String name = getName(state);
            CompoundTag states = new CompoundTag();

            for (BlockMapper blockMapper : BlockMapper.ALL_MAPPERS) {
                blockMapper.apply(state, states);
            }

            NbtMap map = NbtMap.builder()
                .putString("name", "minecraft:" + name)
                .putCompound("states", (NbtMap) NbtOps.INSTANCE.convertTo(CloudburstNbtOps.INSTANCE, states))
                .build();

            // Verify states exist in the provided vanilla block palette
            if (!vanillaStates.contains(map)) {
                throw new RuntimeException("Unknown block state: " + name + " state: " + blockStateToString(state) + " our bedrock state: " + map);
            }
            newMappings.add(Pair.of(state, new BlockEntry(name, states)));
        }

        DataResult<JsonElement> generatorResult = BlockEntry.GENERATOR_CODEC.encodeStart(BlockEntry.JSON_OPS_WITH_BYTE_BOOLEAN, newMappings);
        generatorResult.ifSuccess(json -> {
            JsonObject rootObject = new JsonObject();
            rootObject.add("mappings", json);
            rootObject.addProperty("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            try {
                FileWriter writer = new FileWriter("new_generator_blocks.json");
                builder.create().toJson(rootObject, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).ifError(error -> {
            System.out.println("Failed to save mappings generator copy of blocks mappings.");
            System.out.println(error.message());
        });

        List<BlockEntry> entries = newMappings.stream()
                .map(pair -> {
                    String javaIdentifier = BuiltInRegistries.BLOCK.getKey(pair.key().getBlock()).getPath();
                    String bedrockIdentifier = pair.value().bedrockIdentifier();
                    if (javaIdentifier.equals(bedrockIdentifier)) {
                        // We don't need to store the name if it's the same between both platforms
                        return Pair.of(pair.key(), new BlockEntry(null, pair.value().state()));
                    }
                    return pair;
                })
                .map(Pair::value)
                .toList();
        DataResult<Tag> result = BlockEntry.LIST_CODEC.encodeStart(NbtOps.INSTANCE, entries);
        result.ifSuccess(tag -> {
            try {
                CompoundTag rootTag = new CompoundTag();
                rootTag.put("bedrock_mappings", tag);
                NbtIo.writeCompressed(rootTag, Path.of("mappings").resolve("blocks.nbt"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            System.out.println("Finished writing blocks NBT!");
        }).ifError(error -> {
            System.out.println("Failed to encode blocks to NBT!");
            System.out.println(error.message());
        });

        System.out.println("Finished block writing process!");
    }

    private static String getName(BlockState state) {
        if (NAME_OVERRIDES.containsKey(state.getBlock())) {
            return NAME_OVERRIDES.get(state.getBlock());
        }

        if (STATE_BLOCK_OVERRIDES.containsKey(state.getBlock())) {
            return STATE_BLOCK_OVERRIDES.get(state.getBlock()).apply(state);
        }

        Block block = state.getBlock();
        if (block instanceof BedBlock) {
            return "bed";
        }

        if (block instanceof FlowerPotBlock) {
            return "flower_pot";
        }

        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath();

        if (block instanceof StandingSignBlock) {
            return blockName.replace("sign", "standing_sign");
        }

        if (block instanceof WallHangingSignBlock || block instanceof WallSkullBlock) {
            return blockName.replace("wall_", "");
        }

        if (block instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            if (blockName.contains("cut_copper")) {
                return blockName.replace("cut", "double_cut");
            }
            return blockName.replace("slab", "double_slab");
        }

        if (block instanceof AbstractBannerBlock) {
            if (block instanceof WallBannerBlock) {
                return "wall_banner";
            } else {
                return "standing_banner";
            }
        }

        return blockName;
    }

    public static String blockStateToString(BlockState blockState) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
        if (!blockState.getValues().isEmpty()) {
            stringBuilder.append('[');
            stringBuilder.append(blockState.getValues().entrySet().stream().map(PROPERTY_MAP_PRINTER).collect(Collectors.joining(",")));
            stringBuilder.append(']');
        }
        return stringBuilder.toString();
    }

    private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<>() {

        public String apply(Map.Entry<Property<?>, Comparable<?>> entry) {
            if (entry == null) {
                return "<NULL>";
            } else {
                Property<?> lv = entry.getKey();
                return lv.getName() + "=" + this.nameValue(lv, entry.getValue());
            }
        }

        private <T extends Comparable<T>> String nameValue(Property<T> arg, Comparable<?> comparable) {
            //noinspection unchecked
            return arg.getName((T) comparable);
        }
    };
}
