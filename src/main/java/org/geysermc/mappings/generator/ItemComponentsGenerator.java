package org.geysermc.mappings.generator;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.geysermc.mappings.definitions.item.ItemComponents;
import org.geysermc.mappings.definitions.item.RuntimeItemStates;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.resources.BedrockSamples;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ItemComponentsGenerator extends MappingsGenerator<Map<Identifier, CompoundTag>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompletableFuture<BedrockSamples> bedrockSamples;

    public ItemComponentsGenerator(PackOutput output, CompletableFuture<BedrockSamples> bedrockSamples) {
        super(output, FileType.ITEM_COMPONENTS);
        this.bedrockSamples = bedrockSamples;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return bedrockSamples.thenCompose(samples -> samples.data().open(ItemComponents::read)).thenCompose(components -> {
            List<Identifier> additionalOffhandItems = new ObjectArrayList<>();

            for (Identifier bedrockItem : components.components().keySet()) {
                CompoundTag itemComponents = components.components().get(bedrockItem);
                if (bedrockItem.getPath().equals("fishing_rod")) {
                    // Fix the damage being unequal between the two versions
                    // Maybe. Didn't implement it yet since it actually changes how it's treated over the network
                    continue;
                }

                RuntimeItemStates.State runtimeState = components.states().states().get(bedrockItem);
                if (runtimeState == null) {
                    LOGGER.error("Bedrock item {} defined in item components has no runtime state!", bedrockItem);
                } else if (runtimeState.version() == 1) {
                    // Unsupported version
                    continue;
                }

                itemComponents.getCompound("item_properties").ifPresent(itemProperties -> {
                    itemProperties.putBoolean("allow_off_hand", true);
                    additionalOffhandItems.add(bedrockItem);
                });
            }

            if (additionalOffhandItems.isEmpty()) {
                LOGGER.warn("No Bedrock items found that can be modified to allow offhand usage");
            }

            return CompletableFuture.allOf(saveFile(cache, components.components()), saveFile(cache, FileType.ADDITIONAL_OFFHAND_ITEMS, additionalOffhandItems));
        });
    }

    @Override
    public String getName() {
        return "Item Components Generator";
    }
}
