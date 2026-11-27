package org.geysermc.mappings.definitions.block;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.FileSystemAccess;
import org.geysermc.mappings.util.MappingsCodecs;
import org.geysermc.mappings.util.MappingsUtil;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class BlockMappings {
    public static final Codec<Map<BlockState, BlockEntry>> CODEC = BlockEntry.CODEC.listOf().fieldOf("bedrock_mappings")
            .xmap(BlockMappings::listToMappings, BlockMappings::mappingsToList)
            .codec();
    public static final Codec<Map<BlockState, BlockEntry>> DEBUG_CODEC = Codec.unboundedMap(MappingsCodecs.BLOCK_STATE_STRING_CODEC, BlockEntry.CODEC);
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<BlockState, BlockEntry> mappings = new Object2ObjectLinkedOpenHashMap<>();
    private final BedrockBlocks bedrockBlocks;

    private BlockMappings(Map<BlockState, BlockEntry> mappings, BedrockBlocks bedrockBlocks) {
        this.mappings.putAll(mappings);
        this.bedrockBlocks = bedrockBlocks;
    }

    public static CompletableFuture<BlockMappings> readBlocks(FileSystemAccess samples) {
        // Not reading existing mappings as they're not used for anything (unlike e.g. item mappings)
        return samples.readFileOrThrow(FileType.BEDROCK_BLOCKS).thenApply(blocks -> new BlockMappings(Map.of(), blocks));
    }

    public Map<BlockState, BlockEntry> mappings() {
        return mappings;
    }

    public void map(BlockState state, BlockEntry entry) {
        mappings.put(state, entry);
    }

    public void mapAllStates(Function<BlockState, BlockEntry> mapper) {
        boolean success = true;
        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            BlockEntry mapped = mapper.apply(state);
            if (bedrockBlocks.hasBlockWithState(mapped.getName(state.getBlock()), mapped.state())) {
                map(state, mapped);
            } else {
                success = false;
                LOGGER.error("Unknown bedrock block state: {} for Java state {}", mapped.getName(state.getBlock()), MappingsUtil.blockStateToString(state));
                if (!mapped.state().isEmpty()) {
                    LOGGER.error("Bedrock block state NBT: {}", mapped.state());
                }
            }
        }

        if (!success) {
            LOGGER.error("Errors occurred whilst mapping block states, block mappings are incomplete!");
        }
    }

    private static Map<BlockState, BlockEntry> listToMappings(List<BlockEntry> entries) {
        return IntStream.range(0, entries.size())
                .mapToObj(i -> Pair.of(Block.BLOCK_STATE_REGISTRY.byIdOrThrow(i), entries.get(i)))
                .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));
    }

    private static List<BlockEntry> mappingsToList(Map<BlockState, BlockEntry> mappings) {
        return mappings.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> Block.BLOCK_STATE_REGISTRY.getId(entry.getKey())))
                .map(Map.Entry::getValue)
                .toList();
    }
}
