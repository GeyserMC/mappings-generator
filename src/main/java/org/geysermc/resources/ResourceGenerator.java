package org.geysermc.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.DyeColor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResourceGenerator {

    public static final Map<String, BlockEntry> BLOCK_ENTRIES = new HashMap<>();
    public static final Map<String, ItemEntry> ITEM_ENTRIES = new HashMap<>();
    private static final List<MiningToolItem> MINING_TOOL_ITEMS = new ArrayList<>();

    public void generateBlocks() {
        try {
            File file = new File("mappings/blocks.json");
            if (!file.exists()) {
                System.out.println("Could not find mappings submodule! Did you clone them?");
                return;
            }

            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, BlockEntry>>() {}.getType();
            Map<String, BlockEntry> map = gson.fromJson(new FileReader(file), mapType);
            BLOCK_ENTRIES.putAll(map);

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            FileWriter writer = new FileWriter(file);
            JsonObject rootObject = new JsonObject();

            for (BlockState blockState : getFullBlockDataList()) {
                rootObject.add(blockStateToString(blockState), getRemapBlock(blockState, blockStateToString(blockState)));
            }

            builder.create().toJson(rootObject, writer);
            writer.close();
            System.out.println("Finished block writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void generateItems() {
        try {
            File file = new File("mappings/items.json");
            if (!file.exists()) {
                System.out.println("Could not find mappings submodule! Did you clone them?");
                return;
            }

            try {
                Gson gson = new Gson();
                Type mapType = new TypeToken<Map<String, ItemEntry>>() {}.getType();
                Map<String, ItemEntry> map = gson.fromJson(new FileReader(file), mapType);
                ITEM_ENTRIES.putAll(map);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            FileWriter writer = new FileWriter(file);
            JsonObject rootObject = new JsonObject();

            for (Identifier key : Registry.ITEM.getIds()) {
                Optional<Item> item = Registry.ITEM.getOrEmpty(key);
                if (item.isPresent()) {
                    if (item.get() instanceof MiningToolItem) {
                        MiningToolItem miningToolItem = (MiningToolItem) item.get();
                        MINING_TOOL_ITEMS.add(miningToolItem);
                    }
                    rootObject.add(key.getNamespace() + ":" + key.getPath(), getRemapItem(key.getNamespace() + ":" + key.getPath()));
                }
            }

            builder.create().toJson(rootObject, writer);
            writer.close();
            System.out.println("Finished item writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public JsonObject getRemapBlock(BlockState state, String identifier) {
        JsonObject object = new JsonObject();
        if (BLOCK_ENTRIES.containsKey(identifier)) {
            BlockEntry blockEntry = BLOCK_ENTRIES.get(identifier);
            object.addProperty("bedrock_identifier", blockEntry.getBedrockIdentifier());
            object.addProperty("block_hardness", state.getHardness(null, null));
            object.addProperty("can_break_with_hand", state.getMaterial().canBreakByHand());
            MINING_TOOL_ITEMS.forEach(item -> {
                if (item.getMiningSpeed(null, state) != 1.0f) {
                    String itemClassName = item.getClass().getName();
                    String toolType = itemClassName.substring(19, itemClassName.length() -4);
                    object.addProperty("tool_type", toolType.toLowerCase());
                }
            });
            // Removes nbt tags from identifier
            String trimmedIdentifier = identifier.split("\\[")[0];
            // Add tool type for blocks that use shears or sword
            if (trimmedIdentifier.contains("wool")) {
                object.addProperty("tool_type", "shears");
            } else if (trimmedIdentifier.contains("leaves")) {
                object.addProperty("tool_type", "shears");
            } else if (trimmedIdentifier.contains("cobweb")) {
                object.addProperty("tool_type", "sword");
            } else if (trimmedIdentifier.contains("_bed")) {
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
            }

            if (blockEntry.getBedrockStates() != null)
                object.add("bedrock_states", blockEntry.getBedrockStates());
        } else {
            object.addProperty("bedrock_identifier", identifier.split("\\[")[0]);
        }

        return object;
    }

    public JsonObject getRemapItem(String identifier) {
        JsonObject object = new JsonObject();
        if (ITEM_ENTRIES.containsKey(identifier)) {
            ItemEntry itemEntry = ITEM_ENTRIES.get(identifier);
            object.addProperty("bedrock_id", itemEntry.getBedrockId());
            object.addProperty("bedrock_data", itemEntry.getBedrockData());
        } else {
            object.addProperty("bedrock_id", 248); // update block (missing mapping)
            object.addProperty("bedrock_data", 0);
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

    public List<BlockState> getFullBlockDataList() {
        List<BlockState> blockData = new ArrayList<>();
        Registry.BLOCK.forEach(material -> blockData.addAll(getBlockDataList(material)));
        return blockData.stream().sorted(Comparator.comparingInt(this::getID)).collect(Collectors.toList());
    }

    public List<BlockState> getBlockDataList(Block block) {
        return block.getStateManager().getStates();
    }

    public int getID(BlockState blockData) {
        return Block.getRawIdFromState(blockData);
    }

    private String blockStateToString(BlockState blockState) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Registry.BLOCK.getId(blockState.getBlock()).toString());
        if (!blockState.getEntries().isEmpty()) {
            stringBuilder.append('[');
            stringBuilder.append(blockState.getEntries().entrySet().stream().map(PROPERTY_MAP_PRINTER).collect(Collectors.joining(",")));
            stringBuilder.append(']');
        }
        return stringBuilder.toString();
    }

    private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<Map.Entry<Property<?>, Comparable<?>>, String>() {

        public String apply(Map.Entry<Property<?>, Comparable<?>> entry) {
            if (entry == null) {
                return "<NULL>";
            } else {
                Property<?> lv = entry.getKey();
                return lv.getName() + "=" + this.nameValue(lv, entry.getValue());
            }
        }

        private <T extends Comparable<T>> String nameValue(Property<T> arg, Comparable<?> comparable) {
            return arg.name((T) comparable);
        }
    };
}
