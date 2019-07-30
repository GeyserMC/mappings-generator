package org.geysermc.geyserresources;

import com.google.gson.stream.JsonWriter;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.Item;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

public class GeyserResources extends JavaPlugin {

    @Override
    public void onEnable() {
        createJSONData();
    }

    public void createJSONData() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        try {
            File file = new File(getDataFolder(), "java_items.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            JsonWriter jsonWriter = new JsonWriter(new FileWriter(file));
            jsonWriter.beginObject();
            for (MinecraftKey key : IRegistry.ITEM.keySet()) {
                Optional<Item> item = IRegistry.ITEM.getOptional(key);
                if (item.isPresent()) {
                    jsonWriter.name(key.getNamespace() + ":" + key.getKey());
                    jsonWriter.beginObject();
                    jsonWriter.name("protocol_id").value(Item.getId(item.get()));
                    jsonWriter.endObject();
                }
            }

            jsonWriter.endObject();
            jsonWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            File file = new File(getDataFolder(), "java_blocks.json");
            if (!file.exists()) {
                file.createNewFile();
            }

            JsonWriter jsonWriter = new JsonWriter(new FileWriter(file));
            jsonWriter.beginObject();
            for (MinecraftKey key : IRegistry.BLOCK.keySet()) {
                Optional<Block> block = IRegistry.BLOCK.getOptional(key);
                if (block.isPresent()) {
                    jsonWriter.name(key.getNamespace() + ":" + key.getKey());
                    jsonWriter.beginObject();
                    jsonWriter.name("protocol_id").value(Block.REGISTRY_ID.getId(block.get().getBlockData()));
                    jsonWriter.endObject();
                }
            }

            jsonWriter.endObject();
            jsonWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
