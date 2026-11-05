package org.geysermc.mappings.generator;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.names.Renamers;
import org.geysermc.mappings.resources.BedrockSamples;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ParticleMappingsGenerator extends MappingsGenerator<Map<ParticleType<?>, ParticleMapping>> {
    private static final Collection<ParticleType<?>> IGNORED_PARTICLES = Set.of(
            // Impossible?
            ParticleTypes.BLOCK_MARKER,
            // These 2 are seemingly empty on Java
            ParticleTypes.NOXIOUS_GAS_CLOUD,
            ParticleTypes.ENTITY_EFFECT
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompletableFuture<BedrockSamples> bedrockSamples;

    public ParticleMappingsGenerator(PackOutput output, CompletableFuture<BedrockSamples> bedrockSamples) {
        super(output, FileType.PARTICLE_MAPPINGS);
        this.bedrockSamples = bedrockSamples;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return bedrockSamples.thenCompose(samples -> samples.openSamplesRaw(opened -> {
            // Get a list of valid particle IDs from the bedrock samples
            List<Identifier> validParticleIds = new ArrayList<>();
            Path particlesPath = opened.getPath("resource_pack/particles");
            try(DirectoryStream<Path> particles = opened.provider().newDirectoryStream(particlesPath, _ -> true)) {
                particles.forEach(path -> {
                    try {
                        JsonElement json = JsonParser.parseString(Files.readString(path));
                        validParticleIds.add(Identifier.parse(json.getAsJsonObject()
                                .getAsJsonObject("particle_effect")
                                .getAsJsonObject("description")
                                .get("identifier").getAsString()));
                    } catch (IOException exception) {
                        throw new UncheckedIOException("Failed to read particle in bedrock-samples", exception);
                    }
                });
            }

            return validParticleIds;
        }).thenCompose(validParticleIds -> {
            Map<ParticleType<?>, ParticleMapping> mappings = new Object2ObjectOpenHashMap<>();

            // Then, loop over all particles, mapping them to their bedrock counterpart
            for (ParticleType<?> particle : BuiltInRegistries.PARTICLE_TYPE) {
                if (IGNORED_PARTICLES.contains(particle)) {
                    continue;
                }

                Identifier javaId = BuiltInRegistries.PARTICLE_TYPE.getKey(particle);
                Renamers.PARTICLES.get(particle).ifPresentOrElse(mapping -> {
                    if (mapping.identifier().isPresent() && mapping.identifier().get().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                            && !validParticleIds.contains(mapping.identifier().get())) {
                        LOGGER.error("Bedrock particle ID {} for Java particle {} not found in resource pack, ignoring!", mapping.identifier().get(), javaId);
                        return;
                    }

                    mappings.put(particle, mapping);
                }, () -> {
                    if (validParticleIds.contains(BuiltInRegistries.PARTICLE_TYPE.getKey(particle))) {
                        LOGGER.error("No Bedrock particle mapped for {}, but the Bedrock resource pack contains a particle with the same ID", javaId);
                    } else {
                        LOGGER.error("No Bedrock particle mapped for {}", javaId);
                    }
                });
            }

            return saveFile(cache, mappings);
        }));
    }

    @Override
    public String getName() {
        return "Particle Mappings Generator";
    }
}
