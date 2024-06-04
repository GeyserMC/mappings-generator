package org.geysermc.generator.javaclass;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;

import java.lang.reflect.Field;
import java.util.Locale;

public final class GenerateTags {

    public static void main(String[] args) throws IllegalAccessException {
        // Doing it this way since, as of 1.21, there might be some lazy initialization with tags going on.
        printTags("BlockTag", BlockTags.class);
        printTags("ItemTag", ItemTags.class);
        printTags("EnchantmentTag", EnchantmentTags.class);
    }

    private static void printTags(String geyserName, Class<?> clazz) throws IllegalAccessException {
        for (Field field : clazz.getFields()) {
            TagKey<?> key = (TagKey<?>) field.get(null);
            String path = key.location().getPath();
            String fieldName = path.replace("/", "_").toUpperCase(Locale.ROOT);
            System.out.println("public static final " + geyserName + " " + fieldName + " = new " + geyserName + "(\"" + path + "\");");
        }
        System.out.println();
    }
}
