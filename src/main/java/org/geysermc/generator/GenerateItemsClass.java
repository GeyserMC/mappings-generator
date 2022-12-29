package org.geysermc.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GenerateItemsClass {

    public static void main(String[] args) {
        Util.initialize();

        Map<Item, String> classOverrides = new HashMap<>();
        classOverrides.put(Items.COMPASS, "CompassItem");
        classOverrides.put(Items.GOAT_HORN, "GoatHornItem");
        classOverrides.put(Items.TIPPED_ARROW, "TippedArrowItem");
        classOverrides.put(Items.ARROW, "ArrowItem");
        classOverrides.put(Items.MAP, "MapItem");
        classOverrides.put(Items.FILLED_MAP, "FilledMapItem");
        List<Class<? extends Item>> mirroredClasses = List.of(TieredItem.class, DyeItem.class, SpawnEggItem.class, PotionItem.class);

        for (Item item : BuiltInRegistries.ITEM) {
            StringBuilder builder = new StringBuilder("public static final ");
            String clazz = null;

            String tier = null;
            for (Class<? extends Item> aClass : mirroredClasses) {
                if (aClass.isAssignableFrom(item.getClass())) { // Ensures ThrowablePotionItem extends from PotionItem
                    clazz = aClass.getSimpleName();
                    break;
                }
            }
            if (clazz == null) {
                clazz = classOverrides.getOrDefault(item, "Item");
            }

            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            builder.append(clazz)
                    .append(" ")
                    .append(path.toUpperCase(Locale.ROOT))
                    .append(" = register(new ")
                    .append(clazz)
                    .append("(\"")
                    .append(path)
                    .append("\", ");
            if (item instanceof DyeItem) {
                builder.append(((DyeItem) item).getDyeColor().getId())
                        .append(", ");
            }

            builder.append("builder()");

            if (item.getMaxStackSize() != 64) {
                builder.append(".stackSize(")
                        .append(item.getMaxStackSize())
                        .append(")");
            }

            if (item.getMaxDamage() > 0) {
                builder.append(".maxDamage(")
                        .append(item.getMaxDamage())
                        .append(")");
            }

            builder.append("));"); // First bracket is for the item constructor; second is for the register method
            System.out.println(builder.toString());
        }
    }
}
