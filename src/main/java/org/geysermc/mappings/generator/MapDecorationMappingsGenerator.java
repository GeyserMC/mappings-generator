package org.geysermc.mappings.generator;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.definitions.item.BedrockMapDecoration;
import org.geysermc.mappings.names.Renamers;
import org.geysermc.mappings.resources.BedrockSamples;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class MapDecorationMappingsGenerator extends MappingsGenerator<Map<Holder<MapDecorationType>, BedrockMapDecoration.Serialized>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompletableFuture<BedrockSamples> bedrockSamples;

    public MapDecorationMappingsGenerator(PackOutput output, CompletableFuture<BedrockSamples> bedrockSamples) {
        super(output, FileType.MAP_DECORATIONS);
        this.bedrockSamples = bedrockSamples;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return bedrockSamples.thenCompose(samples -> samples.protocol().open(protocol -> protocol.readFileOrThrow(FileType.BEDROCK_MAP_DECORATIONS))
                .thenCompose(bedrockDecorations -> {
                    Map<Holder<MapDecorationType>, BedrockMapDecoration.Serialized> mappings = new Object2ObjectOpenHashMap<>();
                    for (Holder<MapDecorationType> decorationType : BuiltInRegistries.MAP_DECORATION_TYPE.asHolderIdMap()) {
                        try {
                            mappings.put(decorationType, Renamers.MAP_DECORATIONS.get(decorationType).serialize(bedrockDecorations));
                        } catch (IllegalArgumentException exception) {
                            LOGGER.error("Java map decoration {} has no valid bedrock mapping!", decorationType.getRegisteredName());
                        }
                    }
                    return saveFile(cache, mappings);
                }));
    }

    @Override
    public String getName() {
        return "Map Decoration Mappings Generator";
    }
}
