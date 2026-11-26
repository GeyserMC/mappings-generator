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
import org.geysermc.mappings.definitions.sound.JavaSoundEvents;
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
            SoundEvents.PUFFER_FISH_STING, SoundEvents.PLAYER_ATTACK_KNOCKBACK,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.SKELETON_HORSE_AMBIENT_WATER,
            SoundEvents.SKELETON_HORSE_GALLOP_WATER, SoundEvents.SKELETON_HORSE_JUMP_WATER,
            SoundEvents.PLAYER_BREATH, SoundEvents.SKELETON_HORSE_STEP_WATER,
            SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundEvents.ILLUSIONER_MIRROR_MOVE,
            SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundEvents.ILLUSIONER_PREPARE_MIRROR,
            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundEvents.CROP_PLANTED,
            SoundEvents.ARMOR_EQUIP_TURTLE.value(), SoundEvents.ZOMBIE_INFECT,
            SoundEvents.BELL_RESONATE, SoundEvents.LILY_PAD_PLACE
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
        CompletableFuture<JavaSoundEvents> javaEventsFuture = bedrockSamples.thenApplyAsync(samples -> JavaSoundEvents.load(samples.resources()));

        return CompletableFuture.allOf(bedrockSoundsFuture, javaEventsFuture)
                .thenCompose(_ -> {
                    Set<String> validBedrockSounds = bedrockSoundsFuture.join();
                    JavaSoundEvents javaSoundEvents = javaEventsFuture.join();

                    Map<SoundEvent, SoundMappingInstance> mappings = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
                    Transformer.Maybe<String, SoundMapping> transformer = Transformers.SOUND_EVENTS.create(validBedrockSounds);

                    int missed = 0;
                    for (SoundEvent event : BuiltInRegistries.SOUND_EVENT) {
                        String identifier = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(event)).getPath();
                        if (javaSoundEvents.isEmpty(event)) {
                            LOGGER.info("Skipping {} because it's empty on Java", identifier);
                            continue;
                        } else if (MISSING.contains(event)) {
                            LOGGER.warn("Skipping {} because it's missing on Bedrock", identifier);
                            continue;
                        }

                        // First try mapping the identifier normally
                        Optional<? extends SoundMapping> mapping = transformer.apply(identifier);
                        if (mapping.isPresent()) {
                            mappings.put(event, new SoundMappingInstance(mapping.get()));
                        } else {
                            // If no mapping was found, look for aliases
                            Collection<JavaSoundEvents.Alias> aliases = javaSoundEvents.findAlias(event);
                            Optional<? extends SoundMapping> aliased = Optional.empty();
                            JavaSoundEvents.Alias usedAlias = null;
                            for (JavaSoundEvents.Alias alias : aliases) {
                                aliased = transformer.apply(alias.key());
                                if (aliased.isPresent()) {
                                    usedAlias = alias;
                                    break;
                                }
                            }

                            // If an alias was found, map it, else error
                            if (aliased.isPresent()) {
                                mappings.put(event, new SoundMappingInstance(aliased.get(), usedAlias.volume(), usedAlias.pitch()));
                            } else {
                                LOGGER.error("No sound mapped for {}", identifier);
                                missed++;
                            }
                        }
                    }

                    int mapped = 100 - Math.round(((float) missed / BuiltInRegistries.SOUND_EVENT.size()) * 100.0F);
                    LOGGER.error("Missed sound mappings: {} out of {} ({}% mapped)", missed, BuiltInRegistries.SOUND_EVENT.size(), mapped);

                    return saveFile(cache, mappings);
                });
    }

    @Override
    public String getName() {
        return "Sound Mappings Generator";
    }
}
