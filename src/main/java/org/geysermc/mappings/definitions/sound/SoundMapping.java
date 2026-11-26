package org.geysermc.mappings.definitions.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.geysermc.mappings.util.MappingsCodecs;

import java.util.Optional;
import java.util.Set;

public sealed interface SoundMapping {
    MapCodec<SoundMapping> MAP_CODEC = MappingsCodecs.fuzzyCodec(Set.of(PlaySoundMapping.MAP_CODEC, SoundEventMapping.MAP_CODEC,
            LevelEventMapping.MAP_CODEC), SoundMapping::codec);

    static SoundMapping playSound(String sound) {
        return new PlaySoundMapping(sound);
    }

    static SoundMapping soundEvent(SoundEvent event) {
        return new SoundEventMapping(event, Optional.empty());
    }

    static SoundMapping soundEvent(SoundEvent event, SoundData data) {
        return new SoundEventMapping(event, Optional.of(data));
    }

    static SoundMapping levelEvent(LevelEvent event) {
        return new LevelEventMapping(event);
    }

    default boolean validate(Set<String> validIds) {
        return true;
    }

    MapCodec<? extends SoundMapping> codec();

    record PlaySoundMapping(String sound) implements SoundMapping {
        private static final MapCodec<PlaySoundMapping> MAP_CODEC = Codec.STRING
                .xmap(PlaySoundMapping::new, PlaySoundMapping::sound)
                .fieldOf("play_sound");

        @Override
        public boolean validate(Set<String> validIds) {
            return validIds.contains(sound);
        }

        @Override
        public MapCodec<PlaySoundMapping> codec() {
            return MAP_CODEC;
        }
    }

    record SoundEventMapping(SoundEvent event, Optional<SoundData> data) implements SoundMapping {
        private static final MapCodec<SoundEventMapping> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        MappingsCodecs.enumCodec(SoundEvent::values, SoundEvent::getSerializeName).fieldOf("sound_event").forGetter(SoundEventMapping::event),
                        SoundData.CODEC.optionalFieldOf("data").forGetter(SoundEventMapping::data)
                ).apply(instance, SoundEventMapping::new)
        );

        @Override
        public MapCodec<SoundEventMapping> codec() {
            return MAP_CODEC;
        }
    }

    record LevelEventMapping(LevelEvent event) implements SoundMapping {
        private static final MapCodec<LevelEventMapping> MAP_CODEC = MappingsCodecs.LEVEL_EVENT
                .xmap(LevelEventMapping::new, LevelEventMapping::event)
                .fieldOf("level_event");

        @Override
        public MapCodec<LevelEventMapping> codec() {
            return MAP_CODEC;
        }
    }
}
