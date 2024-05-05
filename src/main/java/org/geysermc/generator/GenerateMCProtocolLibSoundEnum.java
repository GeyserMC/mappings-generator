package org.geysermc.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class GenerateMCProtocolLibSoundEnum {

    public static void main(String[] args) {
        Util.initialize();

        StringBuilder finalOutput = new StringBuilder();
        for (int i = 0; i < BuiltInRegistries.SOUND_EVENT.size(); i++) {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.byId(i);
            ResourceLocation resourceLocation = BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);

            String value = resourceLocation.getPath().replace("minecraft:", "");
            String enumName = value.replace(".", "_")
                    .toUpperCase(Locale.ROOT);

            finalOutput.append(enumName + "(\"" + value + "\")");
            if (i != (BuiltInRegistries.SOUND_EVENT.size() - 1)) {
                finalOutput.append(",\n");
            } else {
                finalOutput.append(";");
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("./sounds.txt"));
            writer.write(finalOutput.toString());
            writer.close();
            System.out.println("Finished sounds writing process!");
        } catch (IOException e) {
            System.out.println("Failed to write sounds.txt!");
            e.printStackTrace();
        }
    }
}
