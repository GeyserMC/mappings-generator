package org.geysermc.generator;

import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProcessItemComponents {

    // As of Bedrock 1.21, only items with the item_properties component can have their offhand status toggled.
    public static void main(String[] args) {
        Util.initialize();

        List<String> allOffhandItems = new ArrayList<>();

        CompoundTag tag;
        try {
            tag = NbtIo.readCompressed(Path.of("palettes/item_components.nbt"), NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String key : tag.getAllKeys()) {
            CompoundTag components = tag.getCompound(key);
            if (key.equals("minecraft:fishing_rod")) {
                // Fix the damage being unequal between the two versions
                // Maybe. Didn't implement it yet since it actually changes how it's treated over the network
            }
            if (!components.getAllKeys().contains("item_properties")) {
                continue;
            }
            CompoundTag itemProperties = components.getCompound("item_properties");
            itemProperties.putBoolean("allow_off_hand", true);
            allOffhandItems.add(key);
        }

        try {
            NbtIo.writeCompressed(tag, Path.of("mappings/item_components.nbt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        allOffhandItems.sort(Comparator.naturalOrder());
        try (FileWriter writer = new FileWriter("additional_offhand_items.json")) {
            // Print so users have an idea of which items can be used in the offhand
            new GsonBuilder().setPrettyPrinting().create().toJson(allOffhandItems, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
