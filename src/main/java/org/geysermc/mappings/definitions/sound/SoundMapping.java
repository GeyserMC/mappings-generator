package org.geysermc.mappings.definitions.sound;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Function;

public record SoundMapping(String sound, float volume, float pitch) {
    public static final Codec<SoundMapping> FULL_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("play_sound").forGetter(SoundMapping::sound),
                    Codec.FLOAT.optionalFieldOf("volume", 1.0F).forGetter(SoundMapping::volume),
                    Codec.FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(SoundMapping::pitch)
            ).apply(instance, SoundMapping::new)
    );
    public static final Codec<SoundMapping> CODEC = Codec.either(Codec.STRING, FULL_CODEC)
            .xmap(either -> either.map(SoundMapping::new, Function.identity()),
                    mapping -> mapping.simple() ? Either.left(mapping.sound) : Either.right(mapping));

    public SoundMapping(String sound) {
        this(sound, 1.0F, 1.0F);
    }

    public boolean simple() {
        return volume == 1.0F && pitch == 1.0F;
    }
}
