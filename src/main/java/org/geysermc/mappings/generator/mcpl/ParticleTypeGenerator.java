package org.geysermc.mappings.generator.mcpl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.generator.MappingsGenerator;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class ParticleTypeGenerator extends MappingsGenerator<String> {

    public ParticleTypeGenerator(PackOutput output) {
        super(output, FileType.MCPL_PARTICLE_TYPE);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        // easiest generator ever
        return saveFile(cache, BuiltInRegistries.PARTICLE_TYPE.listElementIds()
                .map(ResourceKey::identifier)
                .map(Identifier::getPath)
                .map(path -> path.toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(",\n")) + ";\n"
        );
    }

    @Override
    public String getName() {
        return "MCPL/Particle Type Generator";
    }
}
