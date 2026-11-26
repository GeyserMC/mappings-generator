package org.geysermc.mappings.definitions.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SoundMappingInstance(SoundMapping mapping, float volume, float pitch) {
    public static final Codec<SoundMappingInstance> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    SoundMapping.MAP_CODEC.forGetter(SoundMappingInstance::mapping),
                    Codec.FLOAT.optionalFieldOf("volume", 1.0F).forGetter(SoundMappingInstance::volume),
                    Codec.FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(SoundMappingInstance::pitch)
            ).apply(instance, SoundMappingInstance::new)
    );

    public SoundMappingInstance(SoundMapping mapping) {
        this(mapping, 1.0F, 1.0F);
    }
}
