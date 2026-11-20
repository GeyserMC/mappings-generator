package org.geysermc.mappings.transformer.instance.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;

import java.util.Set;

public final class SoundMappings {
    private static final Transformer.Maybe<String, String> IDLE_AMBIENT = Transformers.findAndReplace("idle", "ambient");

    public static final Transformer.MaybeCreator<Set<String>, String, SoundMapping> INSTANCE = validIds -> Transformer.maybe(builder -> builder
            .check(mapping -> mapping.validate(validIds))
            // Playsound mappings
            .mapOutput(SoundMapping::playSound)

            // Simply map any sound that has an exact match on Bedrock
            .tryAll(Transformer.identity())

            // Notes have a different prefix
            .transform(Transformers.startsWith("block.note_block"), Transformers.replace("block.note_block", "note"))

            .mark()
            // Try to map all block names to Bedrock first
            .mapInput(Transformers.replaceGroup("block\\.([\\w_]+)\\.[\\w_]+",
                            Transformers.registryElement(BuiltInRegistries.BLOCK).maybeMap(Block::defaultBlockState).maybeMap(Transformers.BLOCKS))
                    .orElse(Transformer.identity()))
            // Catch any exact matches
            .tryAll(Transformer.identity())
            // Try various other things combined if not
            .tryAllPermutations(Transformer.Maybe::maybeChain,
                    // block.stone.break -> stone.break
                    Transformers.findAndReplace("block\\.", ""),
                    // block.stone.break -> break.stone
                    Transformers.findAndReplace("block\\.([\\w_]+)\\.([\\w_]+)", "$2.$1"),
                    // idle -> ambient
                    IDLE_AMBIENT,
                    // block variants that could use their regular sound
                    Transformers.findAndReplace("polished_", ""),
                    Transformers.findAndReplace("_tiles", ""),
                    Transformers.findAndReplace("_block", ""),
                    Transformers.findAndReplace("_bricks", "_brick"),
                    // synonym names for sound types: break -> dig, close -> closed, etc.
                    Transformers.findAndReplace("break", "dig"),
                    Transformers.findAndReplace("close", "closed"),
                    Transformers.findAndReplace("place", "use"),
                    // coral_block -> coral, etc.
                    Transformers.findAndReplace("copper_chest_oxidized", "copper_chest.oxidized"),
                    Transformers.findAndReplace("copper_chest_weathered", "copper_chest.weathered"),
                    Transformers.findAndReplace("enchantment_table", "enchanting_table"),
                    Transformers.findAndReplace("flowering_azalea", "azalea"),
                    Transformers.findAndReplace("hanging_sign", "sign"),
                    Transformers.findAndReplace("ladder", "wood"),
                    Transformers.findAndReplace("metal", "stone"),
                    Transformers.findAndReplace("moss_carpet", "moss"),
                    // try replacing hit with break sound if hit doesn't exist
                    Transformers.findAndReplace("hit", "break"),
                    // if all else fails, try using step sound
                    Transformers.findAndReplace("break|place|hit|fall", "step")
            )
            .reset()

            .tryAll(Transformers.replace("entity.", "mob."))

            .maybeTransform(Transformers.findAndReplace("music\\.[a-z_]+\\.", "music.game."))
            .maybeTransform(Transformers.findAndReplace("music_disc\\.", "record."))
    );

    private SoundMappings() {}
}
