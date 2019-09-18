package org.geysermc.geyserresources;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.Item;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceGenerator {

    public void createJSONData() {
        try {
            File file = new File("java_items.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            JsonObject object = new JsonObject();
            for (MinecraftKey key : IRegistry.ITEM.keySet()) {
                Optional<Item> item = IRegistry.ITEM.getOptional(key);
                if (item.isPresent()) {
                    JsonObject data = new JsonObject();
                    data.addProperty("identifier", key.getNamespace() + ":" + key.getKey());

                    object.add(String.valueOf(Item.getId(item.get())), data);
                }
            }

            FileWriter writer = new FileWriter(file);
            builder.create().toJson(object, writer);
            writer.close();
            System.out.println("Finished item writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            File file = new File("java_blocks.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            GsonBuilder builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            JsonObject object = new JsonObject();
            for (BlockData blockData : getFullBlockDataList()) {
                NamespacedKey key = blockData.getMaterial().getKey();

                JsonObject data = new JsonObject();
                data.addProperty("identifier", key.getNamespace() + ":" + key.getKey());
                data.addProperty("data", blockData.getAsString());
                object.add(String.valueOf(getID(blockData)), data);
            }

            FileWriter writer = new FileWriter(file);
            builder.create().toJson(object, writer);
            writer.close();
            System.out.println("Finished block writing process!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
