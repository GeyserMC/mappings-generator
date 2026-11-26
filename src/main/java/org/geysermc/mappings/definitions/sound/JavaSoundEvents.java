package org.geysermc.mappings.definitions.sound;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.impl.datagen.client.SoundTypeBuilderImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Mojang doesn't have codecs for sounds.json (yet), but Fabric does for their data generator module
@SuppressWarnings("UnstableApiUsage")
public final class JavaSoundEvents {
    private static final Codec<Map<String, SoundTypeBuilderImpl.SoundType>> SOUNDS_CODEC = Codec.unboundedMap(Codec.STRING, SoundTypeBuilderImpl.SoundType.CODEC);

    private final Multimap<SoundEvent, Alias> aliases;
    private final Set<SoundEvent> emptyEvents;

    private JavaSoundEvents(Multimap<SoundEvent, Alias> aliases, Set<SoundEvent> emptyEvents) {
        this.aliases = aliases;
        this.emptyEvents = emptyEvents;
    }

    public Collection<Alias> findAlias(SoundEvent event) {
        return unflatten(aliases.get(event), new ArrayList<>());
    }

    private Collection<Alias> unflatten(Collection<Alias> aliases, List<Alias> current) {
        for (Alias alias : aliases) {
            if (current.contains(alias)) {
                continue;
            }
            current.add(alias);
            unflatten(this.aliases.get(alias.event), current);
        }
        return current;
    }

    public boolean isEmpty(SoundEvent event) {
        return emptyEvents.contains(event);
    }

    public record Alias(SoundEvent event, float volume, float pitch) {

        public String key() {
            return Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(event)).getPath();
        }
    }

    public static JavaSoundEvents load(ResourceManager resources) {
        Map<String, SoundTypeBuilderImpl.SoundType> sounds;
        try (BufferedReader soundsInput = resources.openAsReader(Identifier.withDefaultNamespace("sounds.json"))) {
            sounds = SOUNDS_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(soundsInput).getAsJsonObject()).getOrThrow();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed reading vanilla sounds.json", exception);
        }

        // First gather a map of all sound events using a simple list of raw paths
        Map<SoundEvent, List<String>> rawPaths = new Object2ObjectLinkedOpenHashMap<>();
        sounds.forEach((key, definition) -> getEvent(key)
                .ifPresent(event -> filterPathsForDefaultSettings(definition.sounds()).ifPresent(strings -> rawPaths.put(event, strings))));

        // Then gather aliases/empty events
        Multimap<SoundEvent, Alias> overrides = MultimapBuilder.hashKeys().arrayListValues().build();
        Set<SoundEvent> emptyEvents = new ObjectOpenHashSet<>();
        sounds.forEach((key, definition) -> getEvent(key).ifPresent(event -> {
            if (overrides.containsKey(event)) {
                return;
            } else if (definition.sounds().isEmpty()) {
                emptyEvents.add(event);
                return;
            }

            List<String> paths = rawPaths.get(event);
            if (paths != null && !paths.isEmpty()) {
                // If this sound event uses a list of paths, find all sound events using the same paths
                List<SoundEvent> allEvents = new ArrayList<>();
                rawPaths.forEach((otherEvent, otherPaths) -> {
                    if (otherPaths.equals(paths)) {
                        allEvents.add(otherEvent);
                    }
                });

                if (allEvents.size() > 1) {
                    // If other events also use this list of paths, find the event matching closely with the first path
                    Optional<SoundEvent> closestEvent = findClosestMatch(allEvents, paths.getFirst());
                    closestEvent.ifPresentOrElse(closest -> allEvents.forEach(otherEvent -> {
                        if (!otherEvent.equals(closest)) {
                            overrides.put(otherEvent, new Alias(closest, 1.0F, 1.0F));
                        }
                    }), () -> allEvents.forEach(first -> allEvents.forEach(second -> {
                        // If no closest match was found (all matches were pretty close), alias all to all
                        if (!first.equals(second)) {
                            overrides.put(first, new Alias(second, 1.0F, 1.0F));
                        }
                    })));
                }
            } else if (definition.sounds().size() == 1) {
                // If the definition only has one sound, and it aliases to another sound event, this is an easy case
                SoundTypeBuilderImpl.Entry sound = definition.sounds().getFirst();
                if (sound.type() == SoundTypeBuilder.RegistrationType.SOUND_EVENT) {
                    getEvent(sound.name()).ifPresent(alias -> overrides.put(event, new Alias(alias, sound.volume(), sound.pitch())));
                }
            }
        }));

        return new JavaSoundEvents(overrides, emptyEvents);
    }

    private static Optional<List<String>> filterPathsForDefaultSettings(List<SoundTypeBuilderImpl.Entry> sounds) {
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
                if (strings.contains(sound.name().getPath())) {
                    continue;
                }
                return Optional.empty();
            }
            strings.add(sound.name().getPath());
        }
        return Optional.of(strings);
    }

    private static boolean hasDefaultSettings(SoundTypeBuilderImpl.Entry sound) {
        // Ignoring volume, pitch, weight
        return sound.type() == SoundTypeBuilder.RegistrationType.FILE
                && sound.attenuationDistance() == SoundTypeBuilder.RegistrationBuilder.DEFAULT_ATTENUATION_DISTANCE
                && !sound.stream() && !sound.preload();
    }

    private static Optional<SoundEvent> findClosestMatch(List<SoundEvent> events, String path) {
        LevenshteinDistance distanceInstance = LevenshteinDistance.getDefaultInstance();

        int closestDistance = Integer.MAX_VALUE;
        int furthestDistance = Integer.MIN_VALUE;
        SoundEvent closestEvent = null;
        for (SoundEvent event : events) {
            int distance = distanceInstance.apply(Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(event)).getPath(), path);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEvent = event;
            }
            if (distance > furthestDistance) {
                furthestDistance = distance;
            }
        }

        double distanceFraction = (double) (furthestDistance - closestDistance) / furthestDistance;
        return distanceFraction < 0.2 ? Optional.empty() : Optional.of(closestEvent);
    }

    private static Optional<SoundEvent> getEvent(String identifier) {
        return getEvent(Identifier.parse(identifier));
    }

    private static Optional<SoundEvent> getEvent(Identifier identifier) {
        return BuiltInRegistries.SOUND_EVENT.get(identifier).map(Holder::value);
    }
}
