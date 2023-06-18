package org.geysermc.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;

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
        classOverrides.put(Items.ELYTRA, "ElytraItem");
        classOverrides.put(Items.SHIELD, "ShieldItem");
        classOverrides.put(Items.FISHING_ROD, "FishingRodItem");
        classOverrides.put(Items.ENCHANTED_BOOK, "EnchantedBookItem");
        classOverrides.put(Items.AXOLOTL_BUCKET, "AxolotlBucketItem");
        classOverrides.put(Items.WRITABLE_BOOK, "WritableBookItem");
        classOverrides.put(Items.WRITTEN_BOOK, "WrittenBookItem");
        classOverrides.put(Items.CROSSBOW, "CrossbowItem");
        classOverrides.put(Items.FIREWORK_ROCKET, "FireworkRocketItem");
        classOverrides.put(Items.FIREWORK_STAR, "FireworkStarItem");
        classOverrides.put(Items.PLAYER_HEAD, "PlayerHeadItem");
        classOverrides.put(Items.TROPICAL_FISH_BUCKET, "TropicalFishBucketItem");
        classOverrides.put(Items.BARREL, "ChestItem");
        classOverrides.put(Items.CHEST, "ChestItem");
        classOverrides.put(Items.ENDER_CHEST, "ChestItem");
        classOverrides.put(Items.TRAPPED_CHEST, "ChestItem");
        List<Class<? extends Item>> mirroredClasses = List.of(DyeableArmorItem.class, TieredItem.class, DyeItem.class, SpawnEggItem.class,
                PotionItem.class, ArmorItem.class, BannerItem.class, DyeableHorseArmorItem.class, BoatItem.class);

        for (Item item : BuiltInRegistries.ITEM) {
            StringBuilder builder = new StringBuilder("public static final ");
            String clazz = null;

            for (Class<? extends Item> aClass : mirroredClasses) {
                if (aClass.isAssignableFrom(item.getClass())) { // Ensures ThrowablePotionItem extends from PotionItem
                    clazz = aClass.getSimpleName();
                    break;
                }
            }
            if (item instanceof BlockItem blockItem) {
                if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
                    clazz = "ShulkerBoxItem";
                } else if (blockItem.getBlock() instanceof FlowerBlock) {
                    clazz = "FlowerItem";
                }
            }
            if (clazz == null) {
                clazz = classOverrides.getOrDefault(item, item instanceof BlockItem ? "BlockItem" : "Item");
            }

            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            builder.append("Item ")
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
            if (item instanceof TieredItem) {
                String tier = ((Tiers) ((TieredItem) item).getTier()).name();
                if ("GOLD".equals(tier)) {
                    tier = "GOLDEN";
                } else if ("WOOD".equals(tier)) {
                    tier = "WOODEN";
                }
                builder.append("ToolTier.")
                        .append(tier)
                        .append(", ");
            }
            if (item instanceof ArmorItem) {
                String tier = ((ArmorMaterials) ((ArmorItem) item).getMaterial()).name();
                builder.append("ArmorMaterial.")
                        .append(tier)
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
