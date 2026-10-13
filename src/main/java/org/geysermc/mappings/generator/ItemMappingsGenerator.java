package org.geysermc.mappings.generator;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.mappings.definitions.item.ItemEntry;
import org.geysermc.mappings.definitions.item.ItemMappings;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.names.Renamers;
import org.geysermc.mappings.resources.BedrockSamples;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ItemMappingsGenerator extends MappingsGenerator<Map<Item, ItemEntry>> {
    private final CompletableFuture<BedrockSamples> bedrockSamples;

    public ItemMappingsGenerator(PackOutput output, CompletableFuture<BedrockSamples> bedrockSamples) {
        super(output, FileType.ITEM_MAPPINGS);
        this.bedrockSamples = bedrockSamples;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return bedrockSamples
                .thenCompose(samples -> samples.openData(data -> ItemMappings.read(this, data)))
                .thenCompose(mappings -> {
                    mappings.mapAllItems((key, item) -> getRemapItem(mappings, key, item, Block.byItem(item)));
                    mappings.checkForDuplicates();
                    return saveFile(cache, mappings.mappings());
                });
    }

    private ItemEntry getRemapItem(ItemMappings mappings, Identifier javaIdentifier, Item item, Block block) {
        // Ignore items that require experiments
        if (FeatureFlags.isExperimental(item.requiredFeatures())) {
            return ItemEntry.UNKNOWN;
        }

        ItemEntry base = mappings.computeIfAbsent(item, () -> new ItemEntry(javaIdentifier, 0));
        Identifier bedrockIdentifier = Renamers.ITEMS.forType(item).apply(base.bedrockIdentifier());
        boolean isBlock = block != Blocks.AIR;
        int bedrockData = isBlock ? base.bedrockData() : 0;
        int firstStateId = -1;
        int lastStateId = -1;
        boolean entityPlacer = base.entityPlacer();

        if (isBlock) {
            for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
                if (state.getBlock() == block) {
                    int stateId = Block.getId(state);
                    if (firstStateId == -1) {
                        firstStateId = stateId;
                    }
                    lastStateId = stateId;
                } else if (lastStateId != -1) {
                    break;
                }
            }
        }

        if (item instanceof SpawnEggItem || item instanceof MinecartItem || item instanceof FireworkRocketItem || item instanceof BoatItem) {
            entityPlacer = true;
        }

        return new ItemEntry(bedrockIdentifier, bedrockData, isBlock, firstStateId, lastStateId, entityPlacer);
    }

    @Override
    public String getName() {
        return "Item Mappings Generator";
    }
}
