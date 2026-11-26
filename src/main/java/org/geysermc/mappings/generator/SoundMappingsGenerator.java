package org.geysermc.mappings.generator;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.definitions.sound.SoundEventAliases;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.definitions.sound.SoundMappingInstance;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class SoundMappingsGenerator extends MappingsGenerator<Map<SoundEvent, SoundMappingInstance>> {
    private static final List<SoundEvent> MISSING = List.of(
            SoundEvents.VILLAGER_WORK_FLETCHER, SoundEvents.FISHING_BOBBER_RETRIEVE,
            SoundEvents.SULFUR_CUBE_SMALL_EAT, SoundEvents.PUFFER_FISH_BLOW_OUT,
            SoundEvents.BOAT_PADDLE_LAND, SoundEvents.PUFFER_FISH_BLOW_UP,
            SoundEvents.BOAT_PADDLE_WATER, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
            SoundEvents.GOAT_MILK, SoundEvents.PUFFER_FISH_STING,
            SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundEvents.SKELETON_HORSE_SWIM,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.SKELETON_HORSE_AMBIENT_WATER,
            SoundEvents.SKELETON_HORSE_GALLOP_WATER, SoundEvents.SKELETON_HORSE_JUMP_WATER,
            SoundEvents.PLAYER_BREATH, SoundEvents.SKELETON_HORSE_STEP_WATER,
            SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundEvents.ILLUSIONER_MIRROR_MOVE,
            SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundEvents.ILLUSIONER_PREPARE_MIRROR,
            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundEvents.CROP_PLANTED,
            SoundEvents.ARMOR_EQUIP_TURTLE.value(), SoundEvents.ZOMBIE_INFECT
    );

    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompletableFuture<BedrockSamples> bedrockSamples;

    public SoundMappingsGenerator(PackOutput output, CompletableFuture<BedrockSamples> bedrockSamples) {
        super(output, FileType.SOUND_MAPPINGS);
        this.bedrockSamples = bedrockSamples;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        CompletableFuture<Set<String>> bedrockSoundsFuture = bedrockSamples.thenCompose(samples -> samples.samples().openRaw(opened ->
                JsonParser.parseString(Files.readString(opened.getPath("resource_pack/sounds/sound_definitions.json")))
                        .getAsJsonObject()
                        .getAsJsonObject("sound_definitions")
                        .keySet()));
        CompletableFuture<SoundEventAliases> aliasesFuture = bedrockSamples.thenApplyAsync(samples -> SoundEventAliases.load(samples.resources()));

        return CompletableFuture.allOf(bedrockSoundsFuture, aliasesFuture)
                .thenCompose(_ -> {
                    Set<String> validBedrockSounds = bedrockSoundsFuture.join();
                    SoundEventAliases aliases = aliasesFuture.join();

                    Map<SoundEvent, SoundMappingInstance> mappings = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
                    Transformer.Maybe<String, SoundMapping> transformer = Transformers.SOUND_EVENTS.create(validBedrockSounds);

                    AtomicInteger missed = new AtomicInteger();
                    BuiltInRegistries.SOUND_EVENT.stream().parallel()
                            .forEach(event -> {
                                String identifier = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(event)).getPath();
                                if (aliases.isEmpty(identifier)) {
                                    LOGGER.info("Skipping {} because it's empty on Java", identifier);
                                    return;
                                } else if (MISSING.contains(event)) {
                                    LOGGER.warn("Skipping {} because it's missing on Bedrock", identifier);
                                    return;
                                }

                                Optional<? extends SoundMapping> mapping = transformer.apply(identifier);
                                if (mapping.isPresent()) {
                                    mappings.put(event, new SoundMappingInstance(mapping.get()));
                                } else {
                                    Collection<SoundEventAliases.Alias> alias = aliases.find(identifier);
                                    SoundEventAliases.Alias usedAlias = null;
                                    Optional<? extends SoundMapping> aliased = Optional.empty();
                                    for (SoundEventAliases.Alias a : alias) {
                                        aliased = transformer.apply(a.key()); // TODO volume + picth
                                        if (aliased.isPresent()) {
                                            usedAlias = a;
                                            break;
                                        }
                                    }
                                    if (aliased.isPresent()) {
                                        mappings.put(event, new SoundMappingInstance(aliased.get(), usedAlias.volume(), usedAlias.pitch()));
                                    } else {
                                        LOGGER.error("No sound mapped for {}", identifier);
                                        missed.incrementAndGet();
                                    }
                                }
                            });

                    int mapped = 100 - Math.round(((float) missed.get() / BuiltInRegistries.SOUND_EVENT.size()) * 100.0F);
                    LOGGER.error("Missed sound mappings: {} out of {} ({}% mapped)", missed.get(), BuiltInRegistries.SOUND_EVENT.size(), mapped);

                    return saveFile(cache, mappings);
                });
    }

    @Override
    public String getName() {
        return "Sound Mappings Generator";
    }
}
