package org.geysermc.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.Locale;

public class GenerateMCProtocolLibSoundEnum {

    public static void main(String[] args) {
        Util.initialize();

        for (int i = 0; i < BuiltInRegistries.SOUND_EVENT.size(); i++) {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.byId(i);
            ResourceLocation resourceLocation = BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);

            String value = resourceLocation.getPath().replace("minecraft:", "");
            String enumName = value.replace(".", "_")
                    .toUpperCase(Locale.ROOT);
            System.out.println(enumName + "(\"" + value + "\"),");
        }
    }
}
