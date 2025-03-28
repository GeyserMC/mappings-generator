package org.geysermc.generator.javaclass;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.geysermc.generator.Util;

import java.util.*;

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
        classOverrides.put(Items.WOLF_ARMOR, "WolfArmorItem");
        classOverrides.put(Items.LEATHER_HORSE_ARMOR, "DyeableArmorItem");
        classOverrides.put(Items.OMINOUS_BOTTLE, "OminousBottleItem");
        classOverrides.put(Items.LIGHT, "LightItem");
        List<Class<? extends Item>> mirroredClasses = List.of(DyeItem.class, SpawnEggItem.class,
                PotionItem.class, BannerItem.class, BoatItem.class);

        for (Item item : BuiltInRegistries.ITEM) {
            FieldConstructor constructor = new FieldConstructor("Item");
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
                } else if (blockItem.getBlock() instanceof DecoratedPotBlock) {
                    clazz = "DecoratedPotItem";
                }
            }


            // Filter out mob heads and elytras
            if (item.components().has(DataComponents.EQUIPPABLE) && !(item instanceof BlockItem) && item != Items.ELYTRA) {
                Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
                assert equippable != null;
                // Filter out llama swag
                if (equippable.canBeEquippedBy(EntityType.PLAYER)) {
                    if (equippable.assetId().isPresent()
                            && equippable.assetId().get().location().getPath().equals("leather")) {
                        clazz = "DyeableArmorItem";
                    } else {
                        clazz = "ArmorItem";
                    }
                }
            }

            clazz = classOverrides.getOrDefault(item, clazz); // Needed so WolfArmor applies over ArmorItem
            if (clazz == null) {
                clazz = item instanceof BlockItem ? "BlockItem" : "Item";
            }

            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            constructor.declareFieldName(path).declareClassName(clazz);
            if (!(item instanceof BlockItem blockItem)
                    || !BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).getPath().equals(path)) { // ItemNameBlockItem class = item name is different from block. Plus some other exceptions like powder snow buckets
                constructor.addParameter(FieldConstructor.wrap(path)); // First block will do this for us for block items.
            }
            if (item instanceof DyeItem) {
                constructor.addParameter(((DyeItem) item).getDyeColor().getId());
            }

            constructor.finishParameters();

            if (item.components().has(DataComponents.WEAPON)) {
                double playerDefault = Player.createAttributes().build().getValue(Attributes.ATTACK_DAMAGE);
                ItemAttributeModifiers attributes = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
                AttributeModifier[] collection = attributes.modifiers().stream()
                        .filter(modifier -> modifier.attribute().equals(Attributes.ATTACK_DAMAGE))
                        .map(ItemAttributeModifiers.Entry::modifier).toArray(AttributeModifier[]::new);
                constructor.addMethod("attackDamage", (collection[0]).amount() + playerDefault);
            }

            if (item instanceof BlockItem blockItem) {
                List<String> blocks = new ArrayList<>();
                blocks.add("Blocks." + BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).getPath().toUpperCase(Locale.ROOT));
                Item.BY_BLOCK.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() == item && entry.getKey() != blockItem.getBlock()) // We'll keep the default one first
                        .map(entry -> "Blocks." + BuiltInRegistries.BLOCK.getKey(entry.getKey()).getPath().toUpperCase(Locale.ROOT))
                        .forEach(blocks::add);
                constructor.addExtraParameters(blocks);
            }

            constructor.finish();
            System.out.println(constructor.toString());
        }
    }
}
