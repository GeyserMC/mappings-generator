package org.geysermc.mappings.generator.mcpl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.generator.MappingsGenerator;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class MapIconTypeGenerator extends MappingsGenerator<String> {

    public MapIconTypeGenerator(PackOutput output) {
        super(output, FileType.MCPL_MAP_ICON_TYPE);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        StringBuilder output = new StringBuilder();

        BuiltInRegistries.MAP_DECORATION_TYPE.asHolderIdMap()
                .forEach(type -> output.append(type.unwrapKey().orElseThrow().identifier().getPath().toUpperCase(Locale.ROOT))
                        .append(",\n"));

        return saveFile(cache, output.toString().replaceFirst(",\n$", ";"));
    }

    @Override
    public String getName() {
        return "MCPL/Map Icon Type Generator";
    }
}
