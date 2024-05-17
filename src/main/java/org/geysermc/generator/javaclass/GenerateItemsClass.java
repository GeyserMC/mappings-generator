package org.geysermc.generator.javaclass;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.geysermc.generator.Util;

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
        classOverrides.put(Items.MACE, "MaceItem");
        List<Class<? extends Item>> mirroredClasses = List.of(TieredItem.class, DyeItem.class, SpawnEggItem.class,
                PotionItem.class, ArmorItem.class, BannerItem.class, BoatItem.class);

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
            if (item instanceof ArmorItem armor && armor.getMaterial().is(ArmorMaterials.LEATHER)) {
                clazz = "DyeableArmorItem";
            }
            if (clazz == null) {
                clazz = classOverrides.getOrDefault(item, item instanceof BlockItem ? "BlockItem" : "Item");
            }

            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            constructor.declareFieldName(path).declareClassName(clazz);
            if (item instanceof DyeItem) {
                constructor.addParameter(((DyeItem) item).getDyeColor().getId());
            }
            if (item instanceof TieredItem) {
                String tier = ((Tiers) ((TieredItem) item).getTier()).name();
                if ("GOLD".equals(tier)) {
                    tier = "GOLDEN";
                } else if ("WOOD".equals(tier)) {
                    tier = "WOODEN";
                }
                constructor.addParameter("ToolTier." + tier);
            }
            if (item instanceof ArmorItem) {
                String materialPath = ((ArmorItem) item).getMaterial().value().layers().get(0).texture(false).getPath();
                String tier = materialPath.substring(materialPath.lastIndexOf('/') + 1, materialPath.indexOf('_')).toUpperCase(Locale.ROOT);
                constructor.addParameter("ArmorMaterial." + tier);
            }

            constructor.finishParameters();

            if (item.getDefaultMaxStackSize() != 64) {
                constructor.addMethod("stackSize", item.getDefaultMaxStackSize());
            }

            if (item.components().has(DataComponents.MAX_DAMAGE)) {
                constructor.addMethod("maxDamage", item.components().get(DataComponents.MAX_DAMAGE));
            }

            if (item instanceof TridentItem || item instanceof DiggerItem || item instanceof SwordItem) {
                double playerDefault = Player.createAttributes().build().getValue(Attributes.ATTACK_DAMAGE);
                ItemAttributeModifiers attributes = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
                AttributeModifier[] collection = attributes.modifiers().stream()
                        .filter(modifier -> modifier.attribute().equals(Attributes.ATTACK_DAMAGE))
                        .map(ItemAttributeModifiers.Entry::modifier).toArray(AttributeModifier[]::new);
                constructor.addMethod("attackDamage", (collection[0]).amount() + playerDefault);
            }

            constructor.finish();
            System.out.println(constructor.toString());
        }
    }
}
