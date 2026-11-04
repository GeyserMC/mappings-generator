package org.geysermc.mappings.generator;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ParticleMappingsGenerator extends MappingsGenerator<Map<String, ParticleMapping>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, String> SPECIAL_MCPL_RENAMES = Map.of(
            "TRIAL_SPAWNER_DETECTION", "TRIAL_SPAWNER_DETECTED_PLAYER",
            "TRIAL_SPAWNER_DETECTION_OMINOUS", "TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS"
    );
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

            Map<String, ParticleMapping> mappings = new Object2ObjectOpenHashMap<>();

            // Then, loop over all particles, mapping them to their bedrock counterpart
            for (net.minecraft.core.particles.ParticleType<?> particle : BuiltInRegistries.PARTICLE_TYPE) {
                String mcplName = identifierToMcpl(BuiltInRegistries.PARTICLE_TYPE.getKey(particle));
                Optional<Identifier> bedrockId = Renamers.PARTICLES.get(particle);
                Optional<? extends LevelEventType> eventType = Renamers.PARTICLES_EVENTS.get(particle);
                // We can define our own particles in other namespaces, in e.g. the integrated pack, so don't check for those
                if (bedrockId.isPresent() && bedrockId.get().getNamespace().equals(Identifier.DEFAULT_NAMESPACE) && !validParticleIds.contains(bedrockId.get())) {
                    // TODO throw or error
                    LOGGER.warn("Bedrock particle ID {} for Java particle {} not found in resource pack.", bedrockId.get(), mcplName);
                    bedrockId = Optional.empty();
                }

                if (eventType.isEmpty()) {
                    eventType = Renamers.PARTICLES_LEVEL_EVENTS.get(particle);
                }

                if (bedrockId.isEmpty() && eventType.isEmpty()) {
                    if (validParticleIds.contains(BuiltInRegistries.PARTICLE_TYPE.getKey(particle))) {
                        LOGGER.error("No Bedrock particle mapped for {}, but the Bedrock resource pack contains a particle with the ID {}", mcplName, BuiltInRegistries.PARTICLE_TYPE.getKey(particle));
                    } else {
                        LOGGER.error("No Bedrock particle mapped for {}", mcplName);
                    }
                }

                mappings.put(mcplName, new ParticleMapping(bedrockId, eventType.map(LevelEventType::toString)));
            }

            return saveFile(cache, mappings);
        }));
    }

    private static String identifierToMcpl(Identifier identifier) {
        String pathUppercase = identifier.getPath().toUpperCase(Locale.ROOT);
        return SPECIAL_MCPL_RENAMES.getOrDefault(pathUppercase, pathUppercase);
    }

    @Override
    public String getName() {
        return "Particle Mappings Generator";
    }
}
