package org.geysermc.generator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;
import org.reflections.Reflections;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class MappingsGenerator {

    public static final Map<String, BlockEntry> BLOCK_ENTRIES = new HashMap<>();
    public static final Map<String, ItemEntry> ITEM_ENTRIES = new HashMap<>();
    public static final Map<String, SoundEntry> SOUND_ENTRIES = new HashMap<>();
    public static final Map<String, Integer> RUNTIME_ITEM_IDS = new HashMap<>();
    public static final Map<String, List<String>> STATES = new HashMap<>();
    private static final List<String> POTTABLE_BLOCK_IDENTIFIERS = Arrays.asList("minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:wither_rose", "minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling", "minecraft:jungle_sapling", "minecraft:acacia_sapling", "minecraft:dark_oak_sapling", "minecraft:red_mushroom", "minecraft:brown_mushroom", "minecraft:fern", "minecraft:dead_bush", "minecraft:cactus", "minecraft:bamboo", "minecraft:crimson_fungus", "minecraft:warped_fungus", "minecraft:crimson_roots", "minecraft:warped_roots");
    // This ends up in collision.json
    // collision_index in blocks.json refers to this to prevent duplication
    // This helps to reduce file size
    public static final List<List<List<Double>>> COLLISION_LIST = Lists.newArrayList();

    private static final Gson GSON = new Gson();

    private final Multimap<String, StateMapper<?>> stateMappers = HashMultimap.create();

    public void generateBlocks() {
        Reflections ref = new Reflections("org.geysermc.generator.state.type");
        for (Class<?> clazz : ref.getTypesAnnotatedWith(StateRemapper.class)) {
            try {
                StateMapper<?> stateMapper = (StateMapper<?>) clazz.newInstance();
                this.stateMappers.put(clazz.getAnnotation(StateRemapper.class).value(), stateMapper);
            } catch (InstantiationException | IllegalAccessException e) {
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
            File collision = new File("mappings/collision.json");
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

            for (NbtMap entry : palette) {
                String identifier = entry.getString("name");
                if (!STATES.containsKey(identifier)) {
                    NbtMap states = entry.getCompound("states");
                    List<String> stateKeys = new ArrayList<>(states.keySet());
                    // ignore some useless keys
                    stateKeys.remove("stone_slab_type");
                    STATES.put(identifier, stateKeys);
                }
            }
            // Some State Corrections
            STATES.put("minecraft:attached_pumpkin_stem", Arrays.asList("growth", "facing_direction"));
            STATES.put("minecraft:attached_melon_stem", Arrays.asList("growth", "facing_direction"));

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            FileWriter writer = new FileWriter(mappings);
            JsonObject rootObject = new JsonObject();

            for (BlockState blockState : getAllStates()) {
                rootObject.add(blockStateToString(blockState), getRemapBlock(blockState, blockStateToString(blockState)));
            }

            builder.create().toJson(rootObject, writer);
            writer.close();

            // Write collision types
            writer = new FileWriter(collision);
            builder.create().toJson(COLLISION_LIST, writer);
            writer.close();

            System.out.println("Some block states need to be manually mapped, please search for MANUALMAP in blocks.json, if there are no occurrences you do not need to do anything.");
            System.out.println("Finished block writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void generateItems() {
        try {
            File mappings = new File("mappings/items.json");
            File itemPalette = new File("palettes/runtime_item_states.json");
            if (!mappings.exists()) {
                System.out.println("Could not find mappings submodule! Did you clone them?");
                return;
            }
            if (!itemPalette.exists()) {
                System.out.println("Could not find item palette (runtime_item_states.json), please refer to the README in the palettes directory.");
                return;
            }

            try {
                Type mapType = new TypeToken<Map<String, ItemEntry>>() {}.getType();
                Map<String, ItemEntry> map = GSON.fromJson(new FileReader(mappings), mapType);
                ITEM_ENTRIES.putAll(map);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            try {
                Type listType = new TypeToken<List<PaletteItemEntry>>(){}.getType();
                List<PaletteItemEntry> entries = GSON.fromJson(new FileReader(itemPalette), listType);
                entries.forEach(item -> RUNTIME_ITEM_IDS.put(item.getIdentifier(), item.getLegacy_id()));
                // Fix some discrepancies - identifier is the Java string and ID is the Bedrock number ID
                RUNTIME_ITEM_IDS.put("minecraft:grass", RUNTIME_ITEM_IDS.get("minecraft:tallgrass")); // Conflicts with grass block
                RUNTIME_ITEM_IDS.put("minecraft:snow", RUNTIME_ITEM_IDS.get("minecraft:snow_layer")); // Conflicts with snow block
                RUNTIME_ITEM_IDS.put("minecraft:melon", RUNTIME_ITEM_IDS.get("minecraft:melon_block")); // Conflicts with melon slice
                RUNTIME_ITEM_IDS.put("minecraft:shulker_box", RUNTIME_ITEM_IDS.get("minecraft:undyed_shulker_box"));
                RUNTIME_ITEM_IDS.put("minecraft:stone_stairs", RUNTIME_ITEM_IDS.get("minecraft:normal_stone_stairs")); // Conflicts with cobblestone stairs
                RUNTIME_ITEM_IDS.put("minecraft:stonecutter", RUNTIME_ITEM_IDS.get("minecraft:stonecutter_block")); // Conflicts with, surprisingly, the OLD MCPE stonecutter
                RUNTIME_ITEM_IDS.put("minecraft:map", RUNTIME_ITEM_IDS.get("minecraft:empty_map")); // Conflicts with filled map
                RUNTIME_ITEM_IDS.put("minecraft:item_frame", RUNTIME_ITEM_IDS.get("minecraft:frame"));
                RUNTIME_ITEM_IDS.put("minecraft:globe_banner_pattern", RUNTIME_ITEM_IDS.get("minecraft:banner_pattern"));
                RUNTIME_ITEM_IDS.put("minecraft:trader_llama_spawn_egg", RUNTIME_ITEM_IDS.get("minecraft:llama_spawn_egg"));
                RUNTIME_ITEM_IDS.put("minecraft:zombified_piglin_spawn_egg", RUNTIME_ITEM_IDS.get("minecraft:zombie_pigman_spawn_egg"));
                RUNTIME_ITEM_IDS.put("minecraft:oak_door", RUNTIME_ITEM_IDS.get("minecraft:wooden_door"));

            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            FileWriter writer = new FileWriter(mappings);
            JsonObject rootObject = new JsonObject();

            for (ResourceLocation key : Registry.ITEM.keySet()) {
                Optional<Item> item = Registry.ITEM.getOptional(key);
                item.ifPresent(value ->
                        rootObject.add(key.getNamespace() + ":" + key.getPath(), getRemapItem(
                                key.getNamespace() + ":" + key.getPath(), Block.byItem(value), value.getMaxStackSize())));
            }

            builder.create().toJson(rootObject, writer);
            writer.close();
            System.out.println("Finished item writing process!");

            // Check for duplicate mappings
            Map<JsonElement, String> itemDuplicateCheck = new HashMap<>();
            for (Map.Entry<String, JsonElement> object : rootObject.entrySet()) {
                if (itemDuplicateCheck.containsKey(object.getValue())) {
                    System.out.println("Possible duplicate items (" + object.getKey() + " and " + itemDuplicateCheck.get(object.getValue()) + ") in mappings: " + object.getValue());
                } else {
                    itemDuplicateCheck.put(object.getValue(), object.getKey());
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void generateSounds() {
        try {
            File mappings = new File("mappings/sounds.json");
            if (!mappings.exists()) {
                System.out.println("Could not find mappings submodule! Did you clone them?");
                return;
            }

            try {
                Type mapType = new TypeToken<Map<String, SoundEntry>>() {}.getType();
                Map<String, SoundEntry> map = GSON.fromJson(new FileReader(mappings), mapType);
                SOUND_ENTRIES.putAll(map);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            FileWriter writer = new FileWriter(mappings);
            JsonObject rootObject = new JsonObject();

            for (ResourceLocation key : Registry.SOUND_EVENT.keySet()) {
                Optional<SoundEvent> sound = Registry.SOUND_EVENT.getOptional(key);
                sound.ifPresent(soundEvent -> {
                    SoundEntry soundEntry = SOUND_ENTRIES.get(key.getPath());
                    if (soundEntry == null) {
                        soundEntry = new SoundEntry(key.getPath(), "", -1, null, false);
                    }
                    JsonObject object = (JsonObject) GSON.toJsonTree(soundEntry);
                    if (soundEntry.getExtraData() <= 0 && !key.getPath().equals("block.note_block.harp")) {
                        object.remove("extra_data");
                    }
                    if (soundEntry.getIdentifier() == null || soundEntry.getIdentifier().isEmpty()) {
                        object.remove("identifier");
                    }
                    if (!soundEntry.isLevelEvent()) {
                        object.remove("level_event");
                    }
                    rootObject.add(key.getPath(), object);
                });
            }

            builder.create().toJson(rootObject, writer);
            writer.close();
            System.out.println("Finished sound writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public JsonObject getRemapBlock(BlockState state, String identifier) {
        JsonObject object = new JsonObject();
        BlockEntry blockEntry = null;
        String trimmedIdentifier = identifier.split("\\[")[0];
        if (BLOCK_ENTRIES.containsKey(identifier)) {
            blockEntry = BLOCK_ENTRIES.get(identifier);
            // All walls before 1.16 use the same identifier (cobblestone_wall)
            if (trimmedIdentifier.endsWith("_wall") && !isSensibleWall(trimmedIdentifier)) {
                object.addProperty("bedrock_identifier", "minecraft:cobblestone_wall");
            } else if (trimmedIdentifier.equals("minecraft:powered_rail")) {
                object.addProperty("bedrock_identifier", "minecraft:golden_rail");
            } else if (trimmedIdentifier.equals("minecraft:light")) {
                object.addProperty("bedrock_identifier", "minecraft:light_block");
            } else if (trimmedIdentifier.equals("minecraft:dirt_path")) {
                object.addProperty("bedrock_identifier", "minecraft:grass_path");
            } else {
                object.addProperty("bedrock_identifier", blockEntry.getBedrockIdentifier());
            }
            object.addProperty("block_hardness", state.getDestroySpeed(null, null));
            List<List<Double>> collisionBoxes = Lists.newArrayList();
            try {
                state.getCollisionShape(null, null).toAabbs().forEach(item -> {
                    List<Double> coordinateList = Lists.newArrayList();
                    // Convert Box class to an array of coordinates
                    // They need to be converted from min/max coordinates to centres and sizes
                    coordinateList.add(item.minX + ((item.maxX - item.minX) / 2));
                    coordinateList.add(item.minY + ((item.maxY - item.minY) / 2));
                    coordinateList.add(item.minZ + ((item.maxZ - item.minZ) / 2));

                    coordinateList.add(item.maxX - item.minX);
                    coordinateList.add(item.maxY - item.minY);
                    coordinateList.add(item.maxZ - item.minZ);

                    collisionBoxes.add(coordinateList);
                });
            } catch (NullPointerException e) {
                // Fallback to empty collision when the position is needed to calculate it
            }

            if (!COLLISION_LIST.contains(collisionBoxes)) {
                COLLISION_LIST.add(collisionBoxes);
            }
            // This points to the index of the collision in collision.json
            object.addProperty("collision_index", COLLISION_LIST.lastIndexOf(collisionBoxes));

            try {
                // Ignore water, lava, and fire because players can't pick them
                if (!trimmedIdentifier.equals("minecraft:water") && !trimmedIdentifier.equals("minecraft:lava") && !trimmedIdentifier.equals("minecraft:fire")) {
                    Block block = state.getBlock();
                    ItemStack pickStack = block.getCloneItemStack(null, null, state);
                    String pickStackIdentifier = Registry.ITEM.getKey(pickStack.getItem()).toString();
                    if (!pickStackIdentifier.equals(trimmedIdentifier)) {
                        object.addProperty("pick_item", pickStackIdentifier);
                    }
                }
            } catch (NullPointerException e) {
                // The block's pick item depends on a block entity.
                // Banners and Shulker Boxes both depend on the block entity.
            }
            object.addProperty("can_break_with_hand", !state.requiresCorrectToolForDrops());
            // Removes nbt tags from identifier
            // Add tool type for blocks that use shears or sword
            if (trimmedIdentifier.contains("_bed")) {
                String woolid = trimmedIdentifier.replace("minecraft:", "");
                woolid = woolid.split("_bed")[0].toUpperCase();
                object.addProperty("bed_color", DyeColor.valueOf(woolid).getId());
            } else if (trimmedIdentifier.contains("head") && !trimmedIdentifier.contains("piston") || trimmedIdentifier.contains("skull")) {
                if (!trimmedIdentifier.contains("wall")) {
                    int rotationId = Integer.parseInt(identifier.substring(identifier.indexOf("rotation=") + 9, identifier.indexOf("]")));
                    object.addProperty("skull_rotation", rotationId);
                }
                if (trimmedIdentifier.contains("wither_skeleton")) {
                    object.addProperty("variation", 1);
                } else if (trimmedIdentifier.contains("skeleton")) {
                    object.addProperty("variation", 0);
                } else if (trimmedIdentifier.contains("zombie")) {
                    object.addProperty("variation", 2);
                } else if (trimmedIdentifier.contains("player")) {
                    object.addProperty("variation", 3);
                } else if (trimmedIdentifier.contains("creeper")) {
                    object.addProperty("variation", 4);
                } else if (trimmedIdentifier.contains("dragon")) {
                    object.addProperty("variation", 5);
                }
            } else if (trimmedIdentifier.contains("_banner")) {
                String woolid = trimmedIdentifier.replace("minecraft:", "");
                woolid = woolid.split("_banner")[0].split("_wall")[0].toUpperCase();
                object.addProperty("banner_color", DyeColor.valueOf(woolid).getId());
            } else if (trimmedIdentifier.contains("note_block")) {
                int notepitch = Integer.parseInt(identifier.substring(identifier.indexOf("note=") + 5, identifier.indexOf(",powered")));
                object.addProperty("note_pitch", notepitch);
            } else if (trimmedIdentifier.contains("shulker_box")) {
                object.addProperty("shulker_direction", getDirectionInt(identifier.substring(identifier.indexOf("facing=") + 7, identifier.indexOf("]"))));
            } else if (trimmedIdentifier.contains("chest") && (identifier.contains("type="))) {
                if (identifier.contains("type=left")) {
                    object.addProperty("double_chest_position", "left");
                } else if (identifier.contains("type=right")) {
                    object.addProperty("double_chest_position", "right");
                }
                if (identifier.contains("north")) {
                    object.addProperty("z", false);
                } else if (identifier.contains("south")) {
                    object.addProperty("z", true);
                } else if (identifier.contains("east")) {
                    object.addProperty("x", true);
                } else if (identifier.contains("west")) {
                    object.addProperty("x", false);
                }
            } else if (trimmedIdentifier.endsWith("_slab") && identifier.contains("type=double") && !blockEntry.getBedrockIdentifier().contains("double")) {
                // Fixes 1.16 double slabs
                object.addProperty("bedrock_identifier", blockEntry.getBedrockIdentifier().replace("_slab", "_double_slab"));
            }
        } else {
            // All walls before 1.16 use the same identifier (cobblestone_wall)
            if (trimmedIdentifier.endsWith("_wall") && !isSensibleWall(trimmedIdentifier)) {
                object.addProperty("bedrock_identifier", "minecraft:cobblestone_wall");
            } else {
                object.addProperty("bedrock_identifier", trimmedIdentifier);
            }
        }

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
        String[] states = identifier.substring(identifier.lastIndexOf("[") + 1).replace("]", "").split(",");
        for (String javaState : states) {
            String key = javaState.split("=")[0];
            if (!this.stateMappers.containsKey(key)) {
                continue;
            }
            Collection<StateMapper<?>> stateMappers = this.stateMappers.get(key);

            stateLoop:
            for (StateMapper<?> stateMapper : stateMappers) {
                String[] blockRegex = stateMapper.getClass().getAnnotation(StateRemapper.class).blockRegex();
                if (blockRegex.length != 0) {
                    for (String regex : blockRegex) {
                        if (!trimmedIdentifier.matches(regex)) {
                            continue stateLoop;
                        }
                    }
                }
                String value = javaState.split("=")[1];
                Pair<String, ?> bedrockState = stateMapper.translateState(identifier, value);
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

        String stateIdentifier = trimmedIdentifier;
        if (trimmedIdentifier.endsWith("_wall") && !isSensibleWall(trimmedIdentifier)) {
            stateIdentifier = "minecraft:cobblestone_wall";
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

        // No more manual pottable because I'm angry I don't care how bad the list looks
        if (POTTABLE_BLOCK_IDENTIFIERS.contains(trimmedIdentifier)) {
            object.addProperty("pottable", true);
        }

        if (statesObject.entrySet().size() != 0) {
            if (statesObject.has("wall_block_type") && trimmedIdentifier.contains("blackstone")) {
                statesObject.getAsJsonObject().remove("wall_block_type");
            }
            object.add("bedrock_states", statesObject);
        }

        return object;
    }

    public JsonObject getRemapItem(String identifier, Block block, int stackSize) {
        JsonObject object = new JsonObject();
        if (ITEM_ENTRIES.containsKey(identifier)) {
            ItemEntry itemEntry = ITEM_ENTRIES.get(identifier);
            if (RUNTIME_ITEM_IDS.containsKey(identifier)) {
                object.addProperty("bedrock_id", RUNTIME_ITEM_IDS.get(identifier));
            } else {
                // Deal with items that we replace
                String replacementIdentifier = null;
                switch (identifier.replace("minecraft:", "")) {
                    case "knowledge_book":
                        replacementIdentifier = "book";
                        break;
                    case "tipped_arrow":
                    case "spectral_arrow":
                        replacementIdentifier = "arrow";
                        break;
                    case "debug_stick":
                        replacementIdentifier = "stick";
                        break;
                    case "furnace_minecart":
                        replacementIdentifier = "hopper_minecart";
                        break;
                    default:
                        break;
                }
                if (identifier.endsWith("banner")) { // Don't include banner patterns
                    replacementIdentifier = "banner";
                } else if (identifier.endsWith("bed")) {
                    replacementIdentifier = "bed";
                } else if (identifier.endsWith("_skull") || (identifier.endsWith("_head"))) {
                    replacementIdentifier = "skull";
                }
                if (replacementIdentifier != null) {
                    object.addProperty("bedrock_id", RUNTIME_ITEM_IDS.get("minecraft:" + replacementIdentifier));
                } else {
                    object.addProperty("bedrock_id", itemEntry.getBedrockId());
                }
            }
            boolean isBlock = block != Blocks.AIR;
            object.addProperty("bedrock_data", isBlock ? itemEntry.getBedrockData() : 0);
            if (isBlock) {
                BlockState state = block.defaultBlockState();
                // Fix some render issues - :microjang:
                if (block instanceof WallBlock) {
                    String blockIdentifier = Registry.BLOCK.getKey(block).toString();
                    if (!isSensibleWall(blockIdentifier)) { // Blackstone renders fine
                        // Required for the item to render with the correct type (sandstone, stone brick, etc)
                        state = state.setValue(WallBlock.UP, false);
                    }
                }
                object.addProperty("blockRuntimeId", Block.getId(state));
            }
        } else {
            object.addProperty("bedrock_id", 248); // update block (missing mapping)
            object.addProperty("bedrock_data", 0);
        }
        if (stackSize != 64) {
            object.addProperty("stack_size", stackSize);
        }
        String[] toolTypes = {"sword", "shovel", "pickaxe", "axe", "shears", "hoe"};
        String[] identifierSplit = identifier.split(":")[1].split("_");
        if (identifierSplit.length > 1) {
            Optional<String> optToolType = Arrays.stream(toolTypes).parallel().filter(identifierSplit[1]::equals).findAny();
            if (optToolType.isPresent()) {
                object.addProperty("tool_type", optToolType.get());
                object.addProperty("tool_tier", identifierSplit[0]);
            }
        } else {
            Optional<String> optToolType = Arrays.stream(toolTypes).parallel().filter(identifierSplit[0]::equals).findAny();
            optToolType.ifPresent(s -> object.addProperty("tool_type", s));
        }

        return object;
    }

    public List<BlockState> getAllStates() {
        List<BlockState> states = new ArrayList<>();
        Registry.BLOCK.forEach(block -> states.addAll(block.getStateDefinition().getPossibleStates()));
        return states.stream().sorted(Comparator.comparingInt(Block::getId)).collect(Collectors.toList());
    }

    private String blockStateToString(BlockState blockState) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Registry.BLOCK.getKey(blockState.getBlock()).toString());
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
            return arg.getName((T) comparable);
        }
    };

    /**
     * Converts a Java edition direction string to an byte for Bedrock edition
     * Designed for Shulker boxes, may work for other things
     *
     * @param direction The direction string
     * @return Converted direction byte
     */
    private static byte getDirectionInt(String direction) {
        return (byte) switch (direction) {
            case "down" -> 0;
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> 1;
        };

    }

    /**
     * @return true if this wall can be treated normally and not stupidly
     */
    private static boolean isSensibleWall(String identifier) {
        return identifier.contains("blackstone");
    }
}
