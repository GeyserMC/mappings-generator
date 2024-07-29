package org.geysermc.generator;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
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

public final class BlockGenerator {
    public static final Map<BlockState, BlockEntry> BLOCK_ENTRIES = new HashMap<>();

    /**
     * Java to Bedrock block identifier overrides
     */
    public static final Map<String, String> BLOCK_OVERRIDES = new HashMap<>();

    static {
        // Register BLOCK_OVERRIDES here
    }

    public static final Map<String, List<String>> STATES = new HashMap<>();

    public static void generate() {
        BlockMappers.registerMappers();

        try {
            NbtList<NbtMap> palette;
            File blockPalette = new File("palettes/blockpalette.nbt");
            if (!blockPalette.exists()) {
                System.out.println("Could not find block palette (blockpalette.nbt), please refer to the README in the palettes directory.");
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

            File mappings = new File("generator_blocks.json");
            if (!mappings.exists()) {
                System.out.println("Couldn't find blocks JSON! What is wrong, dear coder?");
                return;
            }

            JsonObject currentRootObject = (JsonObject) JsonParser.parseReader(new FileReader(mappings));
            List<Pair<BlockState, BlockEntry>> currentBlockMappings = BlockEntry.GENERATOR_CODEC.decode(BlockEntry.JSON_OPS_WITH_BYTE_BOOLEAN, currentRootObject.get("mappings"))
                    .result().orElseThrow().getFirst();
            currentBlockMappings.forEach(pair -> BLOCK_ENTRIES.put(pair.key(), pair.value()));

            // Redned, when you see this... I am going down a dark, dark path.
            // If it works sometime in the future, though, it'll be super helpful!
//            int dataVersion = currentRootObject.get("DataVersion").getAsInt();
//            var output = DataFixers.getDataFixer()
//                    .update(References.BLOCK_STATE,
//                            new Dynamic<>(JsonOps.INSTANCE, currentRootObject.getAsJsonArray("mappings")),
//                            dataVersion, SharedConstants.getCurrentVersion().getDataVersion().getVersion());

            for (NbtMap entry : palette) {
                String identifier = entry.getString("name");
                if (!STATES.containsKey(identifier)) {
                    NbtMap states = entry.getCompound("states");
                    List<String> stateKeys = new ArrayList<>(states.keySet());
                    // ignore some useless keys
                    stateKeys.remove("stone_slab_type");
                    stateKeys.remove("stone_type"); // added to minecraft:stone
                    STATES.put(identifier, stateKeys);
                }
            }
            // Some State Corrections
            STATES.put("minecraft:attached_pumpkin_stem", Arrays.asList("growth", "facing_direction"));
            STATES.put("minecraft:attached_melon_stem", Arrays.asList("growth", "facing_direction"));

            List<Pair<BlockState, BlockEntry>> newMappings = new ArrayList<>(Block.BLOCK_STATE_REGISTRY.size());

            for (BlockState blockState : Block.BLOCK_STATE_REGISTRY) {
                newMappings.add(Pair.of(blockState, getRemapBlock(blockState)));
            }

            // Write this in JSON so it's easily human-parseable and Git-diff-able.
            // Well, you know, for the file being 10MB.
            DataResult<JsonElement> generatorResult = BlockEntry.GENERATOR_CODEC.encodeStart(BlockEntry.JSON_OPS_WITH_BYTE_BOOLEAN, newMappings);
            generatorResult.ifSuccess(json -> {
                JsonObject rootObject = new JsonObject();
                rootObject.add("mappings", json);
                rootObject.addProperty("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

                GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
                try {
                    FileWriter writer = new FileWriter("generator_blocks.json");
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

            System.out.println("Some block states need to be manually mapped, please search for MANUALMAP in blocks.json, if there are no occurrences you do not need to do anything.");
            System.out.println("Finished block writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static BlockEntry getRemapBlock(BlockState state) {
        BlockEntry blockEntry = BLOCK_ENTRIES.get(state);
        String trimmedIdentifier = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        Block block = state.getBlock();

        String bedrockIdentifier;
        if (BLOCK_OVERRIDES.containsKey(trimmedIdentifier)) {
            // handle any special cases first
            bedrockIdentifier = BLOCK_OVERRIDES.get(trimmedIdentifier);
        } else if (trimmedIdentifier.endsWith("_wall") && isSensibleWall(trimmedIdentifier)) {
            // All walls before 1.16 use the same identifier (cobblestone_wall)
            // Reset any existing mapping to cobblestone wall
            bedrockIdentifier = trimmedIdentifier.substring("minecraft:".length());
        } else if (trimmedIdentifier.endsWith("_wall")) {
            bedrockIdentifier = "cobblestone_wall";
        } else if (block == Blocks.POWERED_RAIL) {
            bedrockIdentifier = "golden_rail";
        } else if (block == Blocks.DIRT_PATH) {
            bedrockIdentifier = "grass_path";
        } else if (block == Blocks.SMALL_DRIPLEAF) {
            bedrockIdentifier = "small_dripleaf_block";
        } else if (block == Blocks.BIG_DRIPLEAF_STEM) {
            // Includes the head and stem
            bedrockIdentifier = "big_dripleaf";
        } else if (block == Blocks.FLOWERING_AZALEA_LEAVES) {
            bedrockIdentifier = "azalea_leaves_flowered";
        } else if (block == Blocks.ROOTED_DIRT) {
            bedrockIdentifier = "dirt_with_roots";
        } else if (trimmedIdentifier.contains("powder_snow_cauldron") || trimmedIdentifier.contains("water_cauldron")) {
            bedrockIdentifier = "cauldron";
        } else if (block == Blocks.WAXED_COPPER_BLOCK) {
            bedrockIdentifier = "waxed_copper";
        } else if (trimmedIdentifier.contains("candle")) {
            // Resetting old identifiers
            bedrockIdentifier = trimmedIdentifier.substring("minecraft:".length());
        } else if (block == Blocks.DEEPSLATE_REDSTONE_ORE && state.getValue(BlockStateProperties.LIT)) {
            bedrockIdentifier = "lit_deepslate_redstone_ore";
        } else if (trimmedIdentifier.endsWith("_slab") && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            // Fixes 1.16 double slabs
            if (blockEntry != null) {
                if (blockEntry.bedrockIdentifier().contains("double") && !blockEntry.bedrockIdentifier().contains("copper")) {
                    bedrockIdentifier = blockEntry.bedrockIdentifier();
                } else {
                    bedrockIdentifier = formatDoubleSlab(trimmedIdentifier);
                }
            } else {
                bedrockIdentifier = formatDoubleSlab(trimmedIdentifier);
            }
        } else if (block == Blocks.MANGROVE_SIGN) {
            bedrockIdentifier = "mangrove_standing_sign";
        } else if (block == Blocks.TRIPWIRE) {
            bedrockIdentifier = "trip_wire";
        } else if (trimmedIdentifier.startsWith("minecraft:potted")) {
            // Pots are block entities on Bedrock
            bedrockIdentifier = "flower_pot";
        } else if (trimmedIdentifier.endsWith("piston_head")) {
            if (state.getValue(BlockStateProperties.PISTON_TYPE) == PistonType.STICKY) {
                bedrockIdentifier = "sticky_piston_arm_collision";
            } else {
                bedrockIdentifier = "piston_arm_collision";
            }
        } else if (trimmedIdentifier.endsWith("moving_piston")) {
            bedrockIdentifier = "moving_block";
        } else if (trimmedIdentifier.endsWith("note_block")) {
            bedrockIdentifier = "noteblock";
        } else if (trimmedIdentifier.endsWith("_wall_hanging_sign")) {
            // "wall hanging" signs do not exist on BE. they are just hanging signs.
            bedrockIdentifier = trimmedIdentifier.replace("_wall", "").substring("minecraft:".length());
        } else if (trimmedIdentifier.endsWith("_hanging_sign")) {
            bedrockIdentifier = trimmedIdentifier.substring("minecraft:".length());;
        } else if (isSkull(trimmedIdentifier)) {
            bedrockIdentifier = "skull";
        } else if (trimmedIdentifier.endsWith("light_gray_glazed_terracotta")) {
            bedrockIdentifier = "silver_glazed_terracotta";
        } else {
            // Default to trimmed identifier, or the existing identifier
            bedrockIdentifier = blockEntry != null ? blockEntry.bedrockIdentifier() : trimmedIdentifier.replace("minecraft:", "");
        }

        if (bedrockIdentifier.startsWith("stone_slab") || bedrockIdentifier.startsWith("double_stone_slab")) {
            bedrockIdentifier = bedrockIdentifier.replace("stone_slab", "stone_block_slab");
        }

        // BlockEntry#state() should never be null
        CompoundTag bedrockStates = blockEntry != null ? blockEntry.state() : new CompoundTag();

        if (blockEntry != null && STATES.get("minecraft:" + blockEntry.bedrockIdentifier()) != null) {
            List<String> toRemove = new ArrayList<>();
            // Since we now rely on block states being exact after 1.16.100, we need to remove any old states
            for (String key : bedrockStates.getAllKeys()) {
                List<String> states = STATES.get("minecraft:" + blockEntry.bedrockIdentifier());
                if (!states.contains(key) &&
                        !key.contains("stone_slab_type")) { // Ignore the stone slab types since we ignore them above
                    toRemove.add(key);
                }
            }
            for (String key : toRemove) {
                bedrockStates.remove(key);
            }
        } else if (blockEntry != null) {
            System.out.println("States for " + blockEntry.bedrockIdentifier() + " not found!");
        } else {
            System.out.println("Block entry for " + blockStateToString(state) + " is null?");
        }

        for (BlockMapper blockMapper : BlockMapper.ALL_MAPPERS) {
            blockMapper.apply(state, bedrockStates);
        }

        if (block == Blocks.GLOW_LICHEN || block == Blocks.SCULK_VEIN) {
            int bitset = 0;
            if (state.getValue(BlockStateProperties.DOWN)) {
                bitset |= 1;
            }
            if (state.getValue(BlockStateProperties.UP)) {
                bitset |= 1 << 1;
            }
            if (state.getValue(BlockStateProperties.SOUTH)) {
                bitset |= 1 << 2;
            }
            if (state.getValue(BlockStateProperties.WEST)) {
                bitset |= 1 << 3;
            }
            if (state.getValue(BlockStateProperties.NORTH)) {
                bitset |= 1 << 4;
            }
            if (state.getValue(BlockStateProperties.EAST)) {
                bitset |= 1 << 5;
            }
            bedrockStates.putInt("multi_face_direction_bits", bitset);
        }

        else if (trimmedIdentifier.endsWith("_cauldron")) {
            if (trimmedIdentifier.equals("minecraft:lava_cauldron")) {
                // Only one fill level option
                bedrockStates.putInt("fill_level", 6);
            }
        }

        else if (trimmedIdentifier.contains("big_dripleaf")) {
            boolean isHead = !trimmedIdentifier.contains("stem");
            bedrockStates.putBoolean("big_dripleaf_head", isHead);
            if (!isHead) {
                bedrockStates.putString("big_dripleaf_tilt", "none");
            }
        } else if (block == Blocks.MANGROVE_WOOD || block == Blocks.CHERRY_WOOD) {
            // Didn't seem to do anything
            bedrockStates.putBoolean("stripped_bit", false);
        } else if (trimmedIdentifier.contains("azalea_leaves") || trimmedIdentifier.endsWith("mangrove_leaves")) {
            bedrockStates.putBoolean("update_bit", false);
        }

        String stateIdentifier;
        if (trimmedIdentifier.endsWith("_wall") && !isSensibleWall(trimmedIdentifier)) {
            stateIdentifier = "minecraft:cobblestone_wall";
        } else {
            stateIdentifier = bedrockIdentifier;
        }

        List<String> stateKeys = STATES.get(stateIdentifier);
        if (stateKeys != null) {
            stateKeys.forEach(key -> {
                if (trimmedIdentifier.contains("minecraft:shulker_box")) return;
                if (!bedrockStates.contains(key)) {
                    bedrockStates.putString(key, "MANUALMAP");
                }
            });
        }

        if (!bedrockStates.isEmpty()) {
            if (bedrockStates.contains("wall_block_type") && isSensibleWall(trimmedIdentifier)) {
                bedrockStates.remove("wall_block_type");
            }
        }

        return new BlockEntry(bedrockIdentifier, bedrockStates);
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

    /**
     * @return true if this wall can be treated normally and not stupidly
     */
    private static boolean isSensibleWall(String identifier) {
        return identifier.contains("blackstone") || identifier.contains("deepslate") || identifier.contains("mud_brick") || identifier.contains("tuff");
    }

    private static boolean isSkull(String identifier) {
        return identifier.contains("skull") || (identifier.contains("head") && !identifier.contains("piston"));
    }

    /**
     * @return the correct double slab identifier for Bedrock
     */
    private static String formatDoubleSlab(String identifier) {
        if (identifier.contains("double")) {
            return identifier;
        }

        if (identifier.contains("cut_copper")) {
            return identifier.replace("cut", "double_cut").replace("minecraft:", "");
        }
        return identifier.replace("_slab", "_double_slab").replace("minecraft:", "");
    }

    private static <T extends Comparable<T>> String getPropertyName(BlockState state, Property<T> property) {
        T value = state.getValue(property);
        return property.getName(value);
    }
}
