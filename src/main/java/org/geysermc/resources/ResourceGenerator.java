package org.geysermc.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.Item;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceGenerator {

    public static final String VERSION = "1.14";
    public static final Map<String, BlockEntry> BLOCK_ENTRIES = new HashMap<>();
    public static final Map<String, ItemEntry> ITEM_ENTRIES = new HashMap<>();

    public void generateBlocks() {
        File blocksFile = new File("blocks.json"); // old file
        if (!blocksFile.exists()) {
            System.err.println("Could not find blocks.json!");
            System.exit(0);
            return;
        }

        try {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, BlockEntry>>() {}.getType();
            Map<String, BlockEntry> map = gson.fromJson(new FileReader(blocksFile), mapType);
            BLOCK_ENTRIES.putAll(map);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        try {
            File file = new File("mappings/" + VERSION + "/blocks.json");
            if (!file.exists()) {
                file.mkdir();
                file.createNewFile();
            }

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            FileWriter writer = new FileWriter(file);
            JsonObject rootObject = new JsonObject();

            for (BlockData blockData : getFullBlockDataList()) {
                rootObject.add(blockData.getAsString(), getRemapBlock(blockData.getAsString()));
            }

            builder.create().toJson(rootObject, writer);
            writer.close();
            System.out.println("Finished block writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void generateItems() {
        File itemsFile = new File("items.json"); // old file
        if (!itemsFile.exists()) {
            System.err.println("Could not find items.json!");
            System.exit(0);
            return;
        }

        try {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, ItemEntry>>() {}.getType();
            Map<String, ItemEntry> map = gson.fromJson(new FileReader(itemsFile), mapType);
            ITEM_ENTRIES.putAll(map);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        try {
            File file = new File("mappings/" + VERSION + "/items.json");
            if (!file.exists()) {
                file.mkdir();
                file.createNewFile();
            }

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            FileWriter writer = new FileWriter(file);
            JsonObject rootObject = new JsonObject();

            for (MinecraftKey key : IRegistry.ITEM.keySet()) {
                Optional<Item> item = IRegistry.ITEM.getOptional(key);
                if (item.isPresent()) {
                    rootObject.add(key.getNamespace() + ":" + key.getKey(), getRemapItem(key.getNamespace() + ":" + key.getKey()));
                }
            }

            builder.create().toJson(rootObject, writer);
            writer.close();
            System.out.println("Finished item writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public JsonObject getRemapBlock(String identifier) {
        JsonObject object = new JsonObject();
        if (BLOCK_ENTRIES.containsKey(identifier)) {
            BlockEntry blockEntry = BLOCK_ENTRIES.get(identifier);
            object.addProperty("bedrock_identifier", blockEntry.getBedrockIdentifier());
            object.addProperty("bedrock_data", blockEntry.getBedrockData());
        } else {
            object.addProperty("bedrock_identifier", identifier.split("\\[")[0]);
            object.addProperty("bedrock_data", 0);
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

        return object;
    }

    public List<Material> getBlocks() {
        return Stream.of(Material.values()).filter(mat -> !mat.isLegacy()).filter(Material::isBlock).collect(Collectors.toList());
    }

    public List<BlockData> getFullBlockDataList() {
        List<BlockData> blockData = new ArrayList<>();
        getBlocks().forEach(material -> blockData.addAll(getBlockDataList(material)));
        return blockData.stream().sorted(Comparator.comparingInt(this::getID)).collect(Collectors.toList());
    }

    public List<BlockData> getBlockDataList(Material material) {
        if (!material.isBlock()) {
            throw new IllegalArgumentException(MessageFormat.format("Material {0} is not a block", material));
        }
        return CraftMagicNumbers.getBlock(material).getStates().a().stream()
                .map(CraftBlockData::fromData)
                .sorted(Comparator.comparingInt(this::getID)).collect(Collectors.toList());
    }

    public int getID(BlockData blockData) {
        return Block.getCombinedId(((CraftBlockData) blockData).getState());
    }
}
