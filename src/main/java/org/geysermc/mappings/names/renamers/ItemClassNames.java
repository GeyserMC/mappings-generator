package org.geysermc.mappings.names.renamers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.geysermc.mappings.names.TypeRenamer;

import java.util.List;
import java.util.Optional;

public final class ItemClassNames {
    private static final List<Class<? extends Item>> MIRRORED_CLASSES = List.of(DyeItem.class, SpawnEggItem.class,
            PotionItem.class, BannerItem.class, BoatItem.class, MapItem.class);
    public static final TypeRenamer<Item, String> INSTANCE = TypeRenamer.of(ItemClassNames::getClassName, builder -> builder
            .rename(Items.COMPASS, "CompassItem")
            .rename(Items.GOAT_HORN, "GoatHornItem")
            .rename(Items.TIPPED_ARROW, "TippedArrowItem")
            .rename(Items.ARROW, "ArrowItem")
            .rename(Items.MAP, "MapItem")
            .rename(Items.SHIELD, "ShieldItem")
            .rename(Items.FISHING_ROD, "FishingRodItem")
            .rename(Items.ENCHANTED_BOOK, "EnchantedBookItem")
            .rename(Items.AXOLOTL_BUCKET, "AxolotlBucketItem")
            .rename(Items.WRITABLE_BOOK, "WritableBookItem")
            .rename(Items.WRITTEN_BOOK, "WrittenBookItem")
            .rename(Items.CROSSBOW, "CrossbowItem")
            .rename(Items.FIREWORK_ROCKET, "FireworkRocketItem")
            .rename(Items.FIREWORK_STAR, "FireworkStarItem")
            .rename(Items.PLAYER_HEAD, "PlayerHeadItem")
            .rename(Items.TROPICAL_FISH_BUCKET, "TropicalFishBucketItem")
            .rename(Items.WOLF_ARMOR, "WolfArmorItem")
            .rename(Items.LEATHER_HORSE_ARMOR, "DyeableArmorItem")
            .rename(Items.OMINOUS_BOTTLE, "OminousBottleItem")
            .rename(Items.LIGHT, "LightItem"));

    private ItemClassNames() {}

    private static String getClassName(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof ShulkerBoxBlock) {
                return "ShulkerBoxItem";
            } else if (block instanceof DecoratedPotBlock) {
                return "DecoratedPotItem";
            }
        } else {
            // Filter out mob heads and elytras
            Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
            if (equippable != null && item != Items.ELYTRA) {
                // Filter out llama swag
                if (equippable.canBeEquippedBy(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityTypes.PLAYER))) {
                    if (equippable.assetId().isPresent() && equippable.assetId().orElseThrow() == EquipmentAssets.LEATHER) {
                        return "DyeableArmorItem";
                    } else {
                        return "ArmorItem";
                    }
                }
            }
            
        }
        return getMirroredClassName(item).orElseGet(() -> item instanceof BlockItem ? "BlockItem" : "Item");
    }

    private static Optional<String> getMirroredClassName(Item item) {
        for (Class<? extends Item> mirroredClass : MIRRORED_CLASSES) {
            if (mirroredClass.isAssignableFrom(item.getClass())) {
                return Optional.of(mirroredClass.getSimpleName());
            }
        }
        return Optional.empty();
    }
}
