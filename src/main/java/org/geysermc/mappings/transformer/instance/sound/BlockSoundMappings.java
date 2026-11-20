package org.geysermc.mappings.transformer.instance.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;

import java.util.Map;

final class BlockSoundMappings {
    static final Transformer<String, String> BLOCK_NAME_TRANSFORMER = Transformers.replaceGroup("block\\.([\\w_]+)\\.[\\w_]+", Transformers.registryElement(BuiltInRegistries.BLOCK,
            block -> Transformers.BLOCKS.apply(block.defaultBlockState())));
    static final Transformer<String, String> LEGACY_BLOCK_NAMES = Transformer.strong(builder -> builder
            .transform("copper_chest_oxidized", "copper_chest.oxidized")
            .transform("copper_chest_weathered", "copper_chest.weathered")
            .transform("enchantment_table", "enchanting_table")
            .transform("flowering_azalea", "azalea")
            .transform("hanging_sign", "sign")
            .transform("ladder", "wood")
            .transform("metal", "stone")
            .transform("moss_carpet", "moss")
            .transform(Transformers.findAndReplace("polished_", ""))
            .transform(Transformers.findAndReplace("_tiles", ""))
            .transform(Transformers.findAndReplace("_block", ""))
            .transform(Transformers.findAndReplace("_bricks", "_brick"))
            .finish(Transformer.identity()));
    static final Transformer<String, String> LEGACY_BLOCK_SOUND_TYPES = Transformers.replace(Map.of(
            "break", "dig",
            "hit", "break",
            "close", "closed",
            "place", "use"
    ));
    static final Transformer<String, String> BLOCK_SOUND_TYPE_FALLBACK = Transformers.replace(Map.of(
            "fall", "step",
            "hit", "step",
            "slide", "step"
    ));

    private BlockSoundMappings() {}
}
