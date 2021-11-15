package org.geysermc.generator;

import net.minecraft.core.Registry;

import java.util.Locale;

public class GenerateMCProtocolLibSoundEnum {

    public static void main(String[] args) {
        Util.initialize();

        Registry.SOUND_EVENT.keySet().forEach((resourceLocation -> {
            String value = resourceLocation.getPath().replace("minecraft:", "");
            String enumName = value.replace(".", "_")
                    .toUpperCase(Locale.ROOT);
            System.out.println(enumName + "(\"" + value + "\"),");
        }));
    }
}
