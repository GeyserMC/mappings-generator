package org.geysermc.mappings.definitions.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.geysermc.mappings.util.MappingsCodecs;

import java.util.List;
import java.util.SortedMap;
import java.util.stream.Stream;

public record BedrockBlocks(SortedMap<String, BedrockBlockProperty<?>> properties,
                            SortedMap<Identifier, BedrockBlock> blocks) {
    private static final Codec<SortedMap<String, BedrockBlockProperty<?>>> PROPERTIES_CODEC = MappingsCodecs.mapListCodec(BedrockBlockProperty.CODEC.listOf(), BedrockBlockProperty::name);
    public static final Codec<BedrockBlocks> CODEC = PROPERTIES_CODEC.dispatch("block_properties", BedrockBlocks::properties, BedrockBlocks::codec);

    // These 2 are missing from mojang-blocks in bedrock-samples
    public static final List<Identifier> MISSING_FROM_SAMPLES = Stream.of("moving_block", "end_gateway")
            .map(Identifier::withDefaultNamespace).toList();

    public boolean hasBlockWithState(Identifier identifier, CompoundTag candidate) {
        if (MISSING_FROM_SAMPLES.contains(identifier) && candidate.isEmpty()) {
            // Only accept empty states for missing blocks since we can't verify their state is accurate
            return true;
        }

        BedrockBlock block = blocks.get(identifier);
        if (block == null) {
            return false;
        }
        if (candidate.isEmpty() && block.states().isEmpty()) {
            return true;
        }
        return block.states().stream().anyMatch(state -> state.equals(candidate));
    }

    private static MapCodec<BedrockBlocks> codec(SortedMap<String, BedrockBlockProperty<?>> properties) {
        return MappingsCodecs.mapListCodec(BedrockBlock.codec(properties).listOf(), BedrockBlock::name)
                .fieldOf("data_items")
                .xmap(blocks -> new BedrockBlocks(properties, blocks), BedrockBlocks::blocks);
    }
}
