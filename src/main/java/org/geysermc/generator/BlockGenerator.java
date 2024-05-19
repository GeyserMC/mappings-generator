package org.geysermc.generator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;
import org.reflections.Reflections;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public final class BlockGenerator {
    public static final Map<String, BlockEntry> BLOCK_ENTRIES = new HashMap<>();

    /**
     * Java to Bedrock block identifier overrides
     */
    public static final Map<String, String> BLOCK_OVERRIDES = new HashMap<>();

    static {
        // Register BLOCK_OVERRIDES here
    }

    public static final Map<String, List<String>> STATES = new HashMap<>();

    private static final Gson GSON = new Gson();

    private static final Multimap<String, StateMapper<?>> STATE_MAPPERS = HashMultimap.create();

    public static void generate() {
        Reflections ref = new Reflections("org.geysermc.generator.state.type");
        for (Class<?> clazz : ref.getTypesAnnotatedWith(StateRemapper.class)) {
            try {
                StateMapper<?> stateMapper = (StateMapper<?>) clazz.getDeclaredConstructor().newInstance();
                STATE_MAPPERS.put(clazz.getAnnotation(StateRemapper.class).value(), stateMapper);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                e.printStackTrace();
            }
        }
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

            File mappings = new File("mappings/blocks.json");
            if (!mappings.exists()) {
                System.out.println("Could not find mappings submodule! Did you clone them?");
                return;
            }

            try {
                Type mapType = new TypeToken<Map<String, BlockEntry>>() {}.getType();
                Map<String, BlockEntry> map = GSON.fromJson(new FileReader(mappings), mapType);
                BLOCK_ENTRIES.putAll(map);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            if (true) {
                List<Pair<BlockState, NewBlockEntry>> generatorEntries = new ArrayList<>(BLOCK_ENTRIES.size());
                List<NewBlockEntry> entries = new ArrayList<>(BLOCK_ENTRIES.size());
                for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
                    BlockEntry old = BLOCK_ENTRIES.get(blockStateToString(state));
                    CompoundTag states;
                    if (old.getBedrockStates() != null) {
                        states = (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, old.getBedrockStates());
                    } else {
                        states = null;
                    }

                    String javaIdentifier = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    String bedrockIdentifier = old.getBedrockIdentifier().substring("minecraft:".length());

                    entries.add(new NewBlockEntry(javaIdentifier.equals(bedrockIdentifier) ? null : bedrockIdentifier, states));
                    generatorEntries.add(Pair.of(state, new NewBlockEntry(bedrockIdentifier, states)));
                }

                DataResult<Tag> result = NewBlockEntry.LIST_CODEC.encodeStart(NbtOps.INSTANCE, entries);
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

                // Write this in JSON so it's easily human-parseable and Git-diff-able.
                // Well, you know, for the file being 10MB.
                DataResult<JsonElement> generatorResult = NewBlockEntry.GENERATOR_CODEC.encodeStart(JsonOps.INSTANCE, generatorEntries);
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
                return;
            }

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

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            JsonObject rootObject = new JsonObject();

            for (BlockState blockState : Block.BLOCK_STATE_REGISTRY) {
                rootObject.add(blockStateToString(blockState), getRemapBlock(blockState, blockStateToString(blockState)));
            }

            FileWriter writer = new FileWriter(mappings);
            builder.create().toJson(rootObject, writer);
            writer.close();

            System.out.println("Some block states need to be manually mapped, please search for MANUALMAP in blocks.json, if there are no occurrences you do not need to do anything.");
            System.out.println("Finished block writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static JsonObject getRemapBlock(BlockState state, String identifier) {
        JsonObject object = new JsonObject();
        BlockEntry blockEntry = BLOCK_ENTRIES.get(identifier);
        String trimmedIdentifier = identifier.split("\\[")[0];

        String bedrockIdentifier;
        if (BLOCK_OVERRIDES.containsKey(trimmedIdentifier)) {
            // handle any special cases first
            bedrockIdentifier = BLOCK_OVERRIDES.get(trimmedIdentifier);
        } else if (trimmedIdentifier.endsWith("_wall") && isSensibleWall(trimmedIdentifier)) {
            // All walls before 1.16 use the same identifier (cobblestone_wall)
            // Reset any existing mapping to cobblestone wall
            bedrockIdentifier = trimmedIdentifier;
        } else if (trimmedIdentifier.endsWith("_wall")) {
            bedrockIdentifier = "minecraft:cobblestone_wall";
        } else if (trimmedIdentifier.equals("minecraft:powered_rail")) {
            bedrockIdentifier = "minecraft:golden_rail";
        } else if (trimmedIdentifier.equals("minecraft:light")) {
            bedrockIdentifier = "minecraft:light_block";
        } else if (trimmedIdentifier.equals("minecraft:dirt_path")) {
            bedrockIdentifier = "minecraft:grass_path";
        } else if (trimmedIdentifier.equals("minecraft:small_dripleaf")) {
            bedrockIdentifier = "minecraft:small_dripleaf_block";
        } else if (trimmedIdentifier.equals("minecraft:big_dripleaf_stem")) {
            // Includes the head and stem
            bedrockIdentifier = "minecraft:big_dripleaf";
        } else if (trimmedIdentifier.equals("minecraft:flowering_azalea_leaves")) {
            bedrockIdentifier = "minecraft:azalea_leaves_flowered";
        } else if (trimmedIdentifier.equals("minecraft:rooted_dirt")) {
            bedrockIdentifier = "minecraft:dirt_with_roots";
        } else if (trimmedIdentifier.contains("powder_snow_cauldron") || trimmedIdentifier.contains("water_cauldron")) {
            bedrockIdentifier = "minecraft:cauldron";
        } else if (trimmedIdentifier.equals("minecraft:waxed_copper_block")) {
            bedrockIdentifier = "minecraft:waxed_copper";
        } else if (trimmedIdentifier.contains("candle")) {
            // Resetting old identifiers
            bedrockIdentifier = trimmedIdentifier;
        } else if (identifier.equals("minecraft:deepslate_redstone_ore[lit=true]")) {
            bedrockIdentifier = "minecraft:lit_deepslate_redstone_ore";
        } else if (trimmedIdentifier.endsWith("_slab") && identifier.contains("type=double")) {
            // Fixes 1.16 double slabs
            if (blockEntry != null) {
                if (blockEntry.getBedrockIdentifier().contains("double") && !blockEntry.getBedrockIdentifier().contains("copper")) {
                    bedrockIdentifier = blockEntry.getBedrockIdentifier();
                } else {
                    bedrockIdentifier = formatDoubleSlab(trimmedIdentifier);
                }
            } else {
                bedrockIdentifier = formatDoubleSlab(trimmedIdentifier);
            }
        } else if (trimmedIdentifier.equals("minecraft:mangrove_sign")) {
            bedrockIdentifier = "minecraft:mangrove_standing_sign";
        } else if (trimmedIdentifier.equals("minecraft:tripwire")) {
            bedrockIdentifier = "minecraft:trip_wire";
        } else if (trimmedIdentifier.startsWith("minecraft:potted")) {
            // Pots are block entities on Bedrock
            bedrockIdentifier = "minecraft:flower_pot";
        } else if (trimmedIdentifier.endsWith("piston_head")) {
            if (identifier.contains("type=sticky")) {
                bedrockIdentifier = "minecraft:sticky_piston_arm_collision";
            } else {
                bedrockIdentifier = "minecraft:piston_arm_collision";
            }
        } else if (trimmedIdentifier.endsWith("moving_piston")) {
            bedrockIdentifier = "minecraft:moving_block";
        } else if (trimmedIdentifier.endsWith("note_block")) {
            bedrockIdentifier = "minecraft:noteblock";
        } else if (trimmedIdentifier.endsWith("_wall_hanging_sign")) {
            // "wall hanging" signs do not exist on BE. they are just hanging signs.
            bedrockIdentifier = trimmedIdentifier.replace("_wall", "");
        } else if (trimmedIdentifier.endsWith("_hanging_sign")) {
            bedrockIdentifier = trimmedIdentifier;
        } else if (isSkull(trimmedIdentifier)) {
            bedrockIdentifier = "minecraft:skull";
        } else if (trimmedIdentifier.endsWith("light_gray_glazed_terracotta")) {
            bedrockIdentifier = "minecraft:silver_glazed_terracotta";
        } else {
            // Default to trimmed identifier, or the existing identifier
            bedrockIdentifier = blockEntry != null ? blockEntry.getBedrockIdentifier() : trimmedIdentifier;
        }

        if (bedrockIdentifier.contains(":stone_slab") || bedrockIdentifier.contains(":double_stone_slab")) {
            bedrockIdentifier = bedrockIdentifier.replace("stone_slab", "stone_block_slab");
        }

        object.addProperty("bedrock_identifier", bedrockIdentifier);

        object.addProperty("block_hardness", state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));

        PushReaction pushReaction = state.getPistonPushReaction();
        if (pushReaction != PushReaction.NORMAL) {
            object.addProperty("piston_behavior", pushReaction.toString().toLowerCase());
        }

        if (state.hasBlockEntity()) {
            object.addProperty("has_block_entity", true);
        }

        try {
            // Ignore water, lava, and fire because players can't pick them
            if (!trimmedIdentifier.equals("minecraft:water") && !trimmedIdentifier.equals("minecraft:lava") && !trimmedIdentifier.equals("minecraft:fire")) {
                Block block = state.getBlock();
                ItemStack pickStack = block.getCloneItemStack(EmptyLevelReader.INSTANCE, BlockPos.ZERO, state);
                String pickStackIdentifier = BuiltInRegistries.ITEM.getKey(pickStack.getItem()).toString();
                if (!pickStackIdentifier.equals(trimmedIdentifier)) {
                    object.addProperty("pick_item", pickStackIdentifier);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get clone item stack for " + state);
            e.printStackTrace();
        }
        object.addProperty("can_break_with_hand", !state.requiresCorrectToolForDrops());

        JsonElement bedrockStates = blockEntry != null ? blockEntry.getBedrockStates() : null;
        if (bedrockStates == null) {
            bedrockStates = new JsonObject();
        }

        JsonObject statesObject = bedrockStates.getAsJsonObject();
        if (blockEntry != null && STATES.get(blockEntry.getBedrockIdentifier()) != null) {
            // Prevent ConcurrentModificationException
            List<String> toRemove = new ArrayList<>();
            // Since we now rely on block states being exact after 1.16.100, we need to remove any old states
            for (Map.Entry<String, JsonElement> entry : statesObject.entrySet()) {
                List<String> states = STATES.get(blockEntry.getBedrockIdentifier());
                if (!states.contains(entry.getKey()) &&
                        !entry.getKey().contains("stone_slab_type")) { // Ignore the stone slab types since we ignore them above
                    toRemove.add(entry.getKey());
                }
            }
            for (String key : toRemove) {
                statesObject.remove(key);
            }
        } else if (blockEntry != null) {
            System.out.println("States for " + blockEntry.getBedrockIdentifier() + " not found!");
        } else {
            System.out.println("Block entry for " + blockStateToString(state) + " is null?");
        }
        String[] states = StateMapper.getStates(identifier);
        for (String javaState : states) {
            String key = javaState.split("=")[0];
            if (!STATE_MAPPERS.containsKey(key)) {
                continue;
            }
            Collection<StateMapper<?>> stateMappers = STATE_MAPPERS.get(key);

            stateLoop:
            for (StateMapper<?> stateMapper : stateMappers) {
                String[] blockRegex = stateMapper.getClass().getAnnotation(StateRemapper.class).blockRegex();
                for (String regex : blockRegex) {
                    if (!trimmedIdentifier.matches(regex)) {
                        continue stateLoop;
                    }
                }
                String value = javaState.split("=")[1];
                org.apache.commons.lang3.tuple.Pair<String, ?> bedrockState = stateMapper.translateState(identifier, value);
                if (bedrockState.getValue() instanceof Number) {
                    statesObject.addProperty(bedrockState.getKey(), StateMapper.asType(bedrockState, Number.class));
                }
                if (bedrockState.getValue() instanceof Boolean) {
                    statesObject.addProperty(bedrockState.getKey(), StateMapper.asType(bedrockState, Boolean.class));
                }
                if (bedrockState.getValue() instanceof String) {
                    statesObject.addProperty(bedrockState.getKey(), StateMapper.asType(bedrockState, String.class));
                }
            }
        }

        if (trimmedIdentifier.equals("minecraft:glow_lichen") || trimmedIdentifier.equals("minecraft:sculk_vein")) {
            int bitset = 0;
            List<String> statesList = Arrays.asList(states);
            if (statesList.contains("down=true")) {
                bitset |= 1;
            }
            if (statesList.contains("up=true")) {
                bitset |= 1 << 1;
            }
            if (statesList.contains("south=true")) {
                bitset |= 1 << 2;
            }
            if (statesList.contains("west=true")) {
                bitset |= 1 << 3;
            }
            if (statesList.contains("north=true")) {
                bitset |= 1 << 4;
            }
            if (statesList.contains("east=true")) {
                bitset |= 1 << 5;
            }
            statesObject.addProperty("multi_face_direction_bits", bitset);
        }

        else if (trimmedIdentifier.endsWith("_cauldron")) {
            statesObject.addProperty("cauldron_liquid", trimmedIdentifier.replace("minecraft:", "").replace("_cauldron", ""));
            if (trimmedIdentifier.equals("minecraft:lava_cauldron")) {
                // Only one fill level option
                statesObject.addProperty("fill_level", 6);
            }
        }

        else if (trimmedIdentifier.contains("big_dripleaf")) {
            boolean isHead = !trimmedIdentifier.contains("stem");
            statesObject.addProperty("big_dripleaf_head", isHead);
            if (!isHead) {
                statesObject.addProperty("big_dripleaf_tilt", "none");
            }
        } else if (trimmedIdentifier.equals("minecraft:mangrove_wood") || trimmedIdentifier.equals(("minecraft:cherry_wood"))) {
            // Didn't seem to do anything
            statesObject.addProperty("stripped_bit", false);
        } else if (trimmedIdentifier.contains("azalea_leaves") || trimmedIdentifier.endsWith("mangrove_leaves")) {
            statesObject.addProperty("update_bit", false);
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
                if (!statesObject.has(key)) {
                    statesObject.addProperty(key, "MANUALMAP");
                }
            });
        }

        if (!statesObject.entrySet().isEmpty()) {
            if (statesObject.has("wall_block_type") && isSensibleWall(trimmedIdentifier)) {
                statesObject.getAsJsonObject().remove("wall_block_type");
            }
            object.add("bedrock_states", statesObject);
        }

        return object;
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
            return identifier.replace("cut", "double_cut");
        }
        return identifier.replace("_slab", "_double_slab");
    }
}
