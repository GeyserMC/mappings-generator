package org.geysermc.generator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.generator.state.BlockMapper;
import org.geysermc.generator.state.BlockMappers;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class BlockGenerator {

    public static final Map<Block, String> NAME_OVERRIDES = new HashMap<>();
    public static final Map<Block, Function<BlockState, String>> STATE_BLOCK_OVERRIDES = new HashMap<>();

    static {
        Util.initialize();

        NAME_OVERRIDES.put(Blocks.TEST_BLOCK, "unknown");
        NAME_OVERRIDES.put(Blocks.TEST_INSTANCE_BLOCK, "unknown");
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

        // oak -> wooden / no prefix
        NAME_OVERRIDES.put(Blocks.OAK_SIGN, "standing_sign");
        NAME_OVERRIDES.put(Blocks.OAK_WALL_SIGN, "wall_sign");
        NAME_OVERRIDES.put(Blocks.OAK_DOOR, "wooden_door");
        NAME_OVERRIDES.put(Blocks.OAK_PRESSURE_PLATE, "wooden_pressure_plate");
        NAME_OVERRIDES.put(Blocks.OAK_TRAPDOOR, "trapdoor");
        NAME_OVERRIDES.put(Blocks.OAK_FENCE_GATE, "fence_gate");
        NAME_OVERRIDES.put(Blocks.OAK_BUTTON, "wooden_button");

        NAME_OVERRIDES.put(Blocks.DARK_OAK_SIGN, "darkoak_standing_sign");
        NAME_OVERRIDES.put(Blocks.DARK_OAK_WALL_SIGN, "darkoak_wall_sign");
        NAME_OVERRIDES.put(Blocks.COBBLESTONE_STAIRS, "stone_stairs");
        NAME_OVERRIDES.put(Blocks.NETHER_BRICKS, "nether_brick");

        STATE_BLOCK_OVERRIDES.put(Blocks.DEEPSLATE_REDSTONE_ORE, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "lit_deepslate_redstone_ore";
            }
            return "deepslate_redstone_ore";
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.REDSTONE_TORCH, state -> {
            if (state.getValue(BlockStateProperties.LIT)) {
                return "redstone_torch";
            }
            return "unlit_redstone_torch";
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
                return  "piston_arm_collision";
            }
        });
        STATE_BLOCK_OVERRIDES.put(Blocks.REPEATER, state -> {
            if (state.getValue(BlockStateProperties.POWERED)) {
                return "powered_repeater";
            } else {
                return "unpowered_repeater";
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
            System.out.println("Could not find block palette (block_palette.nbt), please refer to the README in the palettes directory.");
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

        Map<NbtMap, Integer> stateToHash = new HashMap<>();
        for (NbtMap map : palette) {
            var builder = map.toBuilder();
            builder.remove("name_hash");
            builder.remove("version");
            builder.remove("block_id");
            int paletteVersion = (int) builder.remove("network_id");
            stateToHash.put(builder.build(), paletteVersion);
        }

        // These are ordered by java block state runtime ids.
        IntArrayList networkIds = new IntArrayList();

        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            String name = getName(state);
            // unknown blocks aren't hashed
            if (name.equals("unknown")) {
                networkIds.add(-2);
            }

            CompoundTag states = new CompoundTag();

            for (BlockMapper blockMapper : BlockMapper.ALL_MAPPERS) {
                blockMapper.apply(state, states);
            }

            NbtMap map = NbtMap.builder()
                .putString("name", "minecraft:" + name)
                .putCompound("states", (NbtMap) NbtOps.INSTANCE.convertTo(CloudburstNbtOps.INSTANCE, states))
                .build();

            // Now we play matchmaker.
            Integer version = stateToHash.get(map);
            if (version == null) {
                throw new RuntimeException("Unknown block state: " + name + " state: " + blockStateToString(state) + " our bedrock state: " + map);
            }

            networkIds.add(version.intValue());
            System.out.printf("Matched %s to %s (%s) \n", state.getBlock().getDescriptionId(),
                    name, version);
        }

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

        if (block instanceof StandingSignBlock) {
            return BuiltInRegistries.BLOCK.getKey(block).getPath().replace("sign", "standing_sign");
        }

        if (block instanceof WallHangingSignBlock) {
            return BuiltInRegistries.BLOCK.getKey(block).getPath().replace("wall_", "");
        }

        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    static String blockStateToString(BlockState blockState) {
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
