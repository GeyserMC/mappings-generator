package org.geysermc.mappings.generator;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvent;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.definitions.sound.SoundEventAliases;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class SoundMappingsGenerator extends MappingsGenerator<Map<SoundEvent, SoundMapping>> {
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

                    Map<SoundEvent, SoundMapping> mappings = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
                    Transformer.Maybe<String, SoundMapping> transformer = Transformers.SOUND_EVENTS.create(validBedrockSounds);

                    AtomicInteger missed = new AtomicInteger();
                    BuiltInRegistries.SOUND_EVENT.stream().parallel()
                            .forEach(event -> {
                                String identifier = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(event)).getPath();
                                if (aliases.isEmpty(identifier)) {
                                    LOGGER.info("Skipping {} because it's empty on Java", identifier);
                                }

                                Optional<? extends SoundMapping> mapping = transformer.apply(identifier);
                                if (mapping.isPresent()) {
                                    mappings.put(event, mapping.get());
                                } else {
                                    Collection<SoundEventAliases.Alias> alias = aliases.find(identifier);
                                    Optional<? extends SoundMapping> aliased = Optional.empty();
                                    for (SoundEventAliases.Alias a : alias) {
                                        aliased = transformer.apply(a.key()); // TODO volume + picth
                                        if (aliased.isPresent()) {
                                            break;
                                        }
                                    }
                                    if (aliased.isPresent()) {
                                        mappings.put(event, aliased.get());
                                    } else {
                                        LOGGER.error("No sound mapped for {}, closest is {}", identifier, getClosestBedrockSound(validBedrockSounds, identifier));
                                        missed.incrementAndGet();
                                    }
                                }
                            });

                    int mapped = 100 - Math.round(((float) missed.get() / BuiltInRegistries.SOUND_EVENT.size()) * 100.0F);
                    LOGGER.error("Missed sound mappings: {} out of {} ({}% mapped)", missed.get(), BuiltInRegistries.SOUND_EVENT.size(), mapped);

                    return saveFile(cache, mappings);
                });
    }

    private static Optional<String> getPlaySoundMapping(String javaIdentifier, Set<String> bedrockSounds) {
        if (bedrockSounds.contains(javaIdentifier)) {
            return Optional.of(javaIdentifier);
        }

        String identifier = javaIdentifier;
        if (identifier.startsWith("block.note_block")) {
            identifier = "note" + identifier.substring(identifier.lastIndexOf('.'));
        } else if (identifier.startsWith("block.")) {
            identifier = identifier.replace("weeping_vines", "roots");
            identifier = identifier.replace("block.gilded_blackstone.", "block.stone.");
            identifier = identifier.replace("block.metal.", "block.stone.");
            identifier = identifier.replace("block.vine", "block.vines");
            identifier = identifier.replace("small_dripleaf", "big_dripleaf");
            identifier = identifier.replace("rooted_dirt", "dirt_with_roots");
            identifier = identifier.replace("nether_ore", "nether_gold_ore"); // ??? mojang
            identifier = identifier.replace("netherite_block", "netherite");
            identifier = identifier.replace("polished_deepslate", "deepslate");
            identifier = identifier.replace("deepslate_tiles", "deepslate_bricks");
            identifier = identifier.replace("flowering_azalea", "azalea");
            identifier = identifier.replace("frogspawn", "frog_spawn");
            identifier = identifier.replace("moss_carpet", "moss");
            identifier = identifier.replace("nether_bricks", "nether_brick");
            identifier = identifier.replace("wart_block", "nether_wart");

            identifier = identifier.substring("block.".length());
            String[] parts = identifier.split("\\.");
            if (parts.length > 1) {
                identifier = parts[1] + "." + parts[0];
            }
        } else if (identifier.startsWith("item.brush")) {
            String[] parts = identifier.split("\\.");
            identifier = "brush.suspicious_" + parts[3];
        } else if (identifier.startsWith("item.spear.lunge")) {
            // Java: item.spear.lunge_1; Bedrock: item.enchant.lunge1
            char last = identifier.charAt(identifier.length() - 1);
            identifier = "item.enchant.lunge" + last;
        } else if (identifier.startsWith("music.")) {
            // a lot of the bedrock names use "game" instead of overworld or nether
            String[] parts = identifier.split("\\.", 3);
            if (parts.length == 3) {
                identifier = "music.game." + parts[2];
            }
        } else if (identifier.startsWith("entity.")) {
            identifier = identifier.replace("entity.", "mob.");

            if (identifier.contains("donkey")) {
                identifier = identifier.replace("donkey", "horse.donkey");
            } else if (identifier.contains("goat.screaming")) {
                identifier = identifier.replace(".screaming", "");

                String screamer = identifier + ".screamer";
                if (bedrockSounds.contains(screamer)) {
                    identifier = screamer; // specific screamer sound
                }
                // otherwise uses normal goat sound
            } else if (identifier.startsWith("mob.wolf_")) {
                if (identifier.contains("wolf_puglin")) {
                    identifier = identifier.replace("wolf_puglin", "wolf.puglin");
                }

                if (identifier.contains("wolf_sad")) {
                    identifier = identifier.replace("wolf_sad", "wolf.sad");
                }

                if (identifier.contains("wolf_angry")) {
                    identifier = identifier.replace("wolf_angry", "wolf.mad");
                }

                if (identifier.contains("wolf_big")) {
                    identifier = identifier.replace("wolf_big", "wolf.big");
                }

                if (identifier.contains("wolf_cute")) {
                    identifier = identifier.replace("wolf_cute", "wolf.cute");
                }

                if (identifier.contains("wolf_grumpy")) {
                    identifier = identifier.replace("wolf_grumpy", "wolf.grumpy");
                }

                if (identifier.contains(".pant")) {
                    identifier = identifier.replace(".pant", ".panting");
                }

                identifier = identifier.replace("ambient", "bark");
            }
        } else {
            identifier = identifier.replace("item.armor", "armor");
        }

        if (bedrockSounds.contains(identifier)) {
            return Optional.of(identifier);
        }
        return Optional.empty();
    }

    private static @Nullable String getClosestBedrockSound(Set<String> validBedrockSounds, String javaIdentifier) {
        LevenshteinDistance distanceInstance = LevenshteinDistance.getDefaultInstance();

        int closestDistance = Integer.MAX_VALUE;
        String closestBedrockSound = null;
        for (String bedrockSound : validBedrockSounds) {
            int distance = distanceInstance.apply(javaIdentifier, bedrockSound);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestBedrockSound = bedrockSound;
            }
        }
        return closestBedrockSound;
    }

    private static double getPitchAdjust(String javaIdentifier, double fallback) {
        if (javaIdentifier.contains("button") && !javaIdentifier.equals("ui.button.click")) {
            if (javaIdentifier.contains("click_on")) {
                return 0.6;
            } else {
                return 0.5;
            }
        }
        return fallback;
    }

    @Override
    public String getName() {
        return "Sound Mappings Generator";
    }
}
