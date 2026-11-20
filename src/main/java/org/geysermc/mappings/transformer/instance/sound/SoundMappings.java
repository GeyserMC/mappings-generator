package org.geysermc.mappings.transformer.instance.sound;

import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;

import java.util.Optional;
import java.util.Set;

public final class SoundMappings {
    private static final Transformer<String, String> IDLE_AMBIENT = Transformers.replace("idle", "ambient");

    public static final Transformer.Creator<Set<String>, String, Optional<? extends SoundMapping>> INSTANCE = validIds -> Transformer.weak(builder -> builder
            .check(mapping -> mapping.validate(validIds))
            // Playsound mappings
            .mapOutput(SoundMapping::playSound)

            // Simply map any sound that has an exact match on Bedrock
            .catchAll(Transformer.identity())

            // Notes have a different prefix
            .transform(Transformers.startsWith("block.note_block"), Transformers.replace("block.note_block", "note"))

            .mark()
            // Map all block names to Bedrock first
            .mapInput(BlockSoundMappings.BLOCK_NAME_TRANSFORMER)
            // Catch any exact matches
            .catchAll(Transformer.identity())
            // Try various other things combined if not
            .groupedCatch(Transformer::chain,
                    // block.stone.break -> stone.break
                    Transformers.replace("block.", ""),
                    // block.stone.break -> break.stone
                    Transformers.replaceRegex("block\\.([\\w_]+)\\.([\\w_]+)", "$2.$1"),
                    // idle -> ambient
                    IDLE_AMBIENT,
                    // coral_block -> coral, etc.
                    BlockSoundMappings.LEGACY_BLOCK_NAMES,
                    // break -> dig, hit -> break, etc.
                    BlockSoundMappings.LEGACY_BLOCK_SOUND_TYPES,
                    // fall -> step, etc.
                    BlockSoundMappings.BLOCK_SOUND_TYPE_FALLBACK
            )
            .reset()

            .catchAll(Transformers.replace("entity.", "mob."))

            .transform(Transformers.findAndReplace("music\\.[a-z_]+\\.", "music.game."))
            .transform(Transformers.findAndReplace("music_disc\\.", "record."))
    );

    private SoundMappings() {}
}
