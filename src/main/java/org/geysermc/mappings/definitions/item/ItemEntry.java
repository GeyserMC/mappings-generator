package org.geysermc.mappings.definitions.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record ItemEntry(Identifier bedrockIdentifier, int bedrockData, boolean isBlock,
                        int firstBlockRuntimeId, int lastBlockRuntimeId, boolean entityPlacer) {
    public static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("bedrock_identifier").forGetter(ItemEntry::bedrockIdentifier),
                    Codec.INT.optionalFieldOf("bedrock_data", 0).forGetter(ItemEntry::bedrockData),
                    Codec.BOOL.optionalFieldOf("is_block", false).forGetter(ItemEntry::isBlock),
                    Codec.INT.optionalFieldOf("firstBlockRuntimeId", -1).forGetter(ItemEntry::firstBlockRuntimeId),
                    Codec.INT.optionalFieldOf("lastBlockRuntimeId", -1).forGetter(ItemEntry::lastBlockRuntimeId),
                    Codec.BOOL.optionalFieldOf("is_entity_placer", false).forGetter(ItemEntry::entityPlacer)

            ).apply(instance, ItemEntry::new)
    );
    public static final ItemEntry UNKNOWN = new ItemEntry(Identifier.withDefaultNamespace("unknown"), 0);

    public ItemEntry(Identifier bedrockIdentifier, int bedrockData) {
        this(bedrockIdentifier, bedrockData, false, -1, -1, false);
    }
}
