package org.geysermc.mappings.transformer.instance;

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
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.geysermc.mappings.transformer.Transformer;

import java.util.List;
import java.util.Optional;

public final class ItemClassNames {
    private static final List<Class<? extends Item>> MIRRORED_CLASSES = List.of(DyeItem.class, SpawnEggItem.class,
            PotionItem.class, BannerItem.class, BoatItem.class, MapItem.class);

    public static final Transformer<Item, String> INSTANCE = Transformer.strong(builder -> builder
            .transform(Items.COMPASS, "CompassItem")
            .transform(Items.GOAT_HORN, "GoatHornItem")
            .transform(Items.TIPPED_ARROW, "TippedArrowItem")
            .transform(Items.ARROW, "ArrowItem")
            .transform(Items.MAP, "MapItem")
            .transform(Items.SHIELD, "ShieldItem")
            .transform(Items.FISHING_ROD, "FishingRodItem")
            .transform(Items.ENCHANTED_BOOK, "EnchantedBookItem")
            .transform(Items.AXOLOTL_BUCKET, "AxolotlBucketItem")
            .transform(Items.WRITABLE_BOOK, "WritableBookItem")
            .transform(Items.WRITTEN_BOOK, "WrittenBookItem")
            .transform(Items.CROSSBOW, "CrossbowItem")
            .transform(Items.FIREWORK_ROCKET, "FireworkRocketItem")
            .transform(Items.FIREWORK_STAR, "FireworkStarItem")
            .transform(Items.PLAYER_HEAD, "PlayerHeadItem")
            .transform(Items.TROPICAL_FISH_BUCKET, "TropicalFishBucketItem")
            .transform(Items.OMINOUS_BOTTLE, "OminousBottleItem")
            .transform(Items.LIGHT, "LightItem")

            .transform(ItemClassNames::getMirroredClassName)

            .instanceOfStrong(BlockItem.class, sub -> sub
                    .mapInput(BlockItem::getBlock)
                    .instanceOf(ShulkerBoxBlock.class, "ShulkerBoxItem")
                    .instanceOf(DecoratedPotBlock.class, "DecoratedPotItem")
                    .finish("BlockItem"))

            .transform(item -> {
                Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
                // Filter out elytras, and llama swags
                return equippable != null && item != Items.ELYTRA && equippable.canBeEquippedBy(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityTypes.PLAYER));
            }, "ArmorItem")

            .finish("Item")
    );

    private ItemClassNames() {}

    private static Optional<String> getMirroredClassName(Item item) {
        for (Class<? extends Item> mirroredClass : MIRRORED_CLASSES) {
            if (mirroredClass.isAssignableFrom(item.getClass())) {
                return Optional.of(mirroredClass.getSimpleName());
            }
        }
        return Optional.empty();
    }
}
