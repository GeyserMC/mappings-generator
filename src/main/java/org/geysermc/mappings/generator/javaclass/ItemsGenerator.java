package org.geysermc.mappings.generator.javaclass;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import org.geysermc.mappings.generator.MappingsGenerator;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.transformer.Transformers;
import org.geysermc.mappings.util.FieldConstructor;
import org.geysermc.mappings.util.MappingsUtil;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ItemsGenerator extends MappingsGenerator<String> {
    private final CompletableFuture<RegistryAccess> registries;

    public ItemsGenerator(PackOutput output, CompletableFuture<RegistryAccess> registries) {
        super(output, FileType.ITEMS);
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(_ -> {
            StringBuilder clazz = new StringBuilder();

            BuiltInRegistries.ITEM.forEach(item -> {
                FieldConstructor constructor = new FieldConstructor("Item");
                String itemClass = Transformers.ITEM_CLASSES.apply(item);

                Identifier key = BuiltInRegistries.ITEM.getKey(item);
                String path = key.getPath();

                constructor.declareFieldName(path).declareClassName(itemClass);

                // ItemNameBlockItem class = item name is different from block. Plus some other exceptions like powder snow buckets
                if (!(item instanceof BlockItem blockItem) || !BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).getPath().equals(path)) {
                    constructor.addParameter(MappingsUtil.wrapInQuotes(path)); // First block will do this for us for block items.
                }
                if (item instanceof DyeItem) {
                    // FIXME - we can access the component in Geyser
                    constructor.addParameter(item.components().getOrDefault(DataComponents.DYE, DyeColor.WHITE).getId());
                }

                constructor.finishParameters();

                ItemAttributeModifiers attributeModifiers = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
                if (attributeModifiers != null) {
                    double base = Player.createAttributes().build().getValue(Attributes.ATTACK_DAMAGE);
                    double attackDamage = attributeModifiers.compute(Attributes.ATTACK_DAMAGE, base, EquipmentSlot.MAINHAND);
                    if (attackDamage != base) {
                        constructor.addMethod("attackDamage", attackDamage);
                    }
                }

                if (item instanceof BlockItem blockItem) {
                    constructor.addExtraParameters(Item.BY_BLOCK.entrySet().stream()
                        .filter(entry -> entry.getValue() == item)
                        .map(Map.Entry::getKey)
                        .sorted(Comparator.<Block>comparingInt(block -> block == blockItem.getBlock() ? 0 : 1) // We'll keep the default one first
                            .thenComparing(BuiltInRegistries.BLOCK::getKey))
                        .map(block -> "Blocks." + BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase(Locale.ROOT))
                        .toList());
                }

                clazz.append(constructor.finish()).append("\n");
            });

            return saveFile(cache, clazz.toString());
        });
    }

    @Override
    public String getName() {
        return "Items Generator";
    }
}
