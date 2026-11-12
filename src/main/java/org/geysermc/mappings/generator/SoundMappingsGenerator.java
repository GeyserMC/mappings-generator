package org.geysermc.mappings.generator;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.util.MappingsUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SoundMappingsGenerator extends MappingsGenerator<Map<SoundEvent, SoundMapping>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompletableFuture<BedrockSamples> bedrockSamples;

    public SoundMappingsGenerator(PackOutput output, CompletableFuture<BedrockSamples> bedrockSamples) {
        super(output, FileType.SOUND_MAPPINGS);
        this.bedrockSamples = bedrockSamples;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return bedrockSamples.thenCompose(samples -> samples.samples().openRaw(opened ->
                        JsonParser.parseString(Files.readString(opened.getPath("resource_pack/sounds/sound_definitions.json")))
                                .getAsJsonObject()
                                .getAsJsonObject("sound_definitions")
                                .keySet()))
                .thenCombine(readExistingFile(), Pair::of).thenCompose(validIdsAndMappings -> {
                    Set<String> validBedrockSounds = validIdsAndMappings.getFirst();
                    Map<SoundEvent, SoundMapping> mappings = new Object2ObjectOpenHashMap<>();
                    validIdsAndMappings.getSecond().ifPresent(mappings::putAll);

                    boolean automaticMappingOfBlockSoundsFailed = false;
                    boolean mappingsAreIncomplete = false;

                    for (SoundEvent soundEvent : BuiltInRegistries.SOUND_EVENT) {
                        Identifier key = BuiltInRegistries.SOUND_EVENT.getKey(soundEvent);
                        assert key != null;
                        String path = key.getPath();

                        SoundMapping mapping = mappings.computeIfAbsent(soundEvent, _ -> SoundMapping.EMPTY);

                        Optional<String> identifier = mapping.identifier();
                        Optional<String> playSoundMapping = getPlaySoundMapping(path, validBedrockSounds)
                                .or(() -> mapping.playSoundMapping().filter(validBedrockSounds::contains));
                        Optional<String> bedrockMapping = mapping.bedrockMapping();
                        int extraData = mapping.extraData();
                        double pitchAdjust = getPitchAdjust(path, mapping.pitchAdjust());
                        boolean levelEvent = mapping.levelEvent();

                        if (playSoundMapping.isEmpty()) {
                            // Auto map place block sounds
                            if (mapping.bedrockMapping().isEmpty() && mapping.identifier().isEmpty()
                                    && path.startsWith("block") && path.endsWith("place")) {
                                LOGGER.info("Attempting to automatically map PLACE sound: {}", key);
                                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:" + path.split("\\.")[1]));
                                bedrockMapping = Optional.of("PLACE");
                                if (block != Blocks.AIR) {
                                    identifier = Optional.of(MappingsUtil.blockStateToString(block.defaultBlockState()));
                                } else {
                                    LOGGER.warn("Unable to automatically map PLACE sound: {}", key);
                                    identifier = Optional.of("MANUALMAP");
                                    automaticMappingOfBlockSoundsFailed = true;
                                }
                            } else {
                                String closestBedrockSound = getClosestBedrockSound(validBedrockSounds, path);
                                LOGGER.warn("Java sound {} has no valid playsound mapping, the closest sound found is {}", key, closestBedrockSound);
                            }
                        }

                        if (levelEvent) {
                            if (bedrockMapping.isPresent()) {
                                try {
                                    LevelEvent.valueOf(bedrockMapping.get());
                                } catch (IllegalArgumentException exception) {
                                    LOGGER.warn("Level event mapping for Java sound {} ({}) does not exist, removing", key, bedrockMapping.get());
                                    bedrockMapping = Optional.empty();
                                    levelEvent = false;
                                }
                            } else {
                                LOGGER.warn("Mapping for Java sound {} is marked as level event, but has no level event specified", key);
                                LOGGER.warn("Setting level_event to false");
                                levelEvent = false;
                            }
                        } else if (bedrockMapping.isPresent()) {
                            try {
                                org.cloudburstmc.protocol.bedrock.data.SoundEvent.valueOf(bedrockMapping.get());
                            } catch (IllegalArgumentException exception) {
                                LOGGER.warn("Sound event mapping for Java sound {} ({}) does not exist, removing", key, bedrockMapping.get());
                                bedrockMapping = Optional.empty();
                            }
                        }

                        if (extraData <= 0 && !path.equals("block.note_block.harp")) {
                            extraData = -1;
                        }

                        if (playSoundMapping.isEmpty() && bedrockMapping.isEmpty()) {
                            LOGGER.error("Java sound {} has no valid sound mapping", key);
                            mappingsAreIncomplete = true;
                        }

                        mappings.put(soundEvent, new SoundMapping(identifier, playSoundMapping, bedrockMapping, extraData, pitchAdjust, levelEvent));
                    }

                    if (automaticMappingOfBlockSoundsFailed) {
                        LOGGER.warn("Some PLACE sounds need to be manually mapped, please search for \"MANUALMAP\" in sounds.json");
                    }
                    if (mappingsAreIncomplete) {
                        LOGGER.error("Sound mappings are incomplete, please check the logs above");
                    }

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
