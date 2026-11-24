package org.geysermc.mappings.definitions.sound;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.impl.datagen.client.SoundTypeBuilderImpl;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// Mojang doesn't have codecs for sounds.json (yet), but Fabric does for their data generator module
@SuppressWarnings("UnstableApiUsage")
public final class SoundEventAliases {
    private static final Codec<Map<String, SoundTypeBuilderImpl.SoundType>> SOUNDS_CODEC = Codec.unboundedMap(Codec.STRING, SoundTypeBuilderImpl.SoundType.CODEC);

    private final Map<String, Alias> map;

    private SoundEventAliases(Map<String, Alias> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    public Optional<Alias> find(String key) {
        return Optional.ofNullable(map.get(key));
    }

    public record Alias(String key, float volume, float pitch) {}

    public static SoundEventAliases load(ResourceManager resources) {
        Map<String, SoundTypeBuilderImpl.SoundType> sounds;
        try (BufferedReader soundsInput = resources.openAsReader(Identifier.withDefaultNamespace("sounds.json"))) {
            sounds = SOUNDS_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(soundsInput).getAsJsonObject()).getOrThrow();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed reading vanilla sounds.json", exception);
        }

        // First gather a map of all sound events using a simple list of raw paths
        Map<String, List<String>> rawPaths = new Object2ObjectOpenHashMap<>();
        sounds.forEach((key, definition)
                -> filterForDefaultSettings(definition.sounds()).ifPresent(strings -> rawPaths.put(filterNamespace(key), strings)));

        // Then gather aliases
        Map<String, Alias> overrides = new Object2ObjectOpenHashMap<>();
        sounds.forEach((key, definition) -> {
            // We're filtering the namespace out here
            key = filterNamespace(key);
            if (overrides.containsKey(key)) {
                return;
            }

            List<String> paths = rawPaths.get(key);
            if (paths != null && !paths.isEmpty()) {
                // If this sound event uses a list of paths, find all sound events using the same paths
                List<String> allEvents = new ArrayList<>();
                rawPaths.forEach((otherEvent, otherPaths) -> {
                    if (otherPaths.equals(paths)) {
                        allEvents.add(otherEvent);
                    }
                });

                if (allEvents.size() > 1) {
                    // If other events also use this list of paths, find the event matching closely with the first path
                    String closestEvent = findClosestMatch(allEvents, paths.getFirst());
                    // And register an override for all other events
                    allEvents.forEach(event -> {
                        if (!event.equals(closestEvent)) {
                            overrides.put(event, new Alias(closestEvent, 1.0F, 1.0F));
                        }
                    });
                }
            } else if (definition.sounds().size() == 1) {
                // If the definition only has one sound, and it aliases to another sound event, this is an easy case
                SoundTypeBuilderImpl.Entry sound = definition.sounds().getFirst();
                if (sound.type() == SoundTypeBuilder.RegistrationType.SOUND_EVENT) {
                    overrides.put(key, new Alias(sound.name().getPath(), sound.volume(), sound.pitch()));
                }
            }
        });

        return new SoundEventAliases(overrides);
    }

    private static String filterNamespace(String key) {
        return Identifier.parse(key).getPath();
    }

    private static Optional<List<String>> filterForDefaultSettings(List<SoundTypeBuilderImpl.Entry> sounds) {
        List<String> strings = new ArrayList<>();
        float volume = -1.0F;
        float pitch = -1.0F;

        for (SoundTypeBuilderImpl.Entry sound : sounds) {
            if (!hasDefaultSettings(sound)) {
                return Optional.empty();
            } else if (volume == -1.0F) {
                volume = sound.volume();
                pitch = sound.pitch();
            } else if (volume != sound.volume() || pitch != sound.pitch()) {
                return Optional.empty();
            }
            strings.add(sound.name().getPath());
        }
        return Optional.of(strings);
    }

    private static boolean hasDefaultSettings(SoundTypeBuilderImpl.Entry sound) {
        return sound.type() == SoundTypeBuilder.RegistrationType.FILE
                && sound.weight() == SoundTypeBuilder.RegistrationBuilder.DEFAULT_WEIGHT
                && sound.attenuationDistance() == SoundTypeBuilder.RegistrationBuilder.DEFAULT_ATTENUATION_DISTANCE
                && !sound.stream() && !sound.preload();
    }

    private static String findClosestMatch(List<String> events, String path) {
        LevenshteinDistance distanceInstance = LevenshteinDistance.getDefaultInstance();

        int closestDistance = Integer.MAX_VALUE;
        String closestEvent = null;
        for (String event : events) {
            int distance = distanceInstance.apply(event, path);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEvent = event;
            }
        }
        return Objects.requireNonNull(closestEvent);
    }
}
