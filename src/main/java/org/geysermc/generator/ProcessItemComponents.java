package org.geysermc.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public final class ProcessItemComponents {

    // As of Bedrock 1.21, only items with the item_properties component can have their offhand status toggled.
    // As of Bedrock 1.21.60, items with the version 1 (DATA_DRIVEN) cannot be modified
    // Those break on the client end.
    // Currently (1.21.60 / 1.21.70) no items can be modified. RIP.
    public static void main(String[] args) {
        Util.initialize();

        final Gson GSON = new Gson();

        List<String> allOffhandItems = new ArrayList<>();
        Map<String, Integer> itemVersion = new HashMap<>();

        File itemPalette = new File("palettes/runtime_item_states.json");
        if (!itemPalette.exists()) {
            System.out.println("Could not find item palette (runtime_item_states.json), please refer to the README in the palettes directory.");
            return;
        }

        CompoundTag tag;
        try {
            tag = NbtIo.readCompressed(Path.of("palettes/item_components.nbt"), NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Type listType = new TypeToken<List<PaletteItemEntry>>(){}.getType();
            List<PaletteItemEntry> entries = GSON.fromJson(new FileReader(itemPalette), listType);
            entries.forEach(entry -> {
                itemVersion.put(entry.getIdentifier(), entry.getVersion());
            });
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        for (String key : tag.keySet()) {
            CompoundTag components = tag.getCompoundOrEmpty(key);
            if (key.equals("minecraft:fishing_rod")) {
                // Fix the damage being unequal between the two versions
                // Maybe. Didn't implement it yet since it actually changes how it's treated over the network
                continue;
            }

            if (itemVersion.get(key) == null) {
                throw new RuntimeException("Unknown item version: " + key);
            }

            if (itemVersion.get(key) == 1) {
                System.out.printf("Ignoring item %s due to unsupported version %s!%n", key, itemVersion.get(key));
                continue;
            }

            if (!components.keySet().contains("item_properties")) {
                continue;
            }
            CompoundTag itemProperties = components.getCompoundOrEmpty("item_properties");
            itemProperties.putBoolean("allow_off_hand", true);
            allOffhandItems.add(key);
        }

        if (allOffhandItems.isEmpty()) {
            System.out.println("No items found that can be modified!");
            return;
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
