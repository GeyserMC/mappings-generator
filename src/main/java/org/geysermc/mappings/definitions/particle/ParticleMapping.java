package org.geysermc.mappings.definitions.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.geysermc.mappings.util.MappingsCodecs;

import java.util.Optional;
import java.util.Set;

public interface ParticleMapping {
    Codec<ParticleMapping> CODEC = MappingsCodecs.fuzzyCodec(Set.of(IdentifierMapping.MAP_CODEC, ParticleTypeMapping.MAP_CODEC,
            LevelEventMapping.MAP_CODEC, BuiltInMapping.MAP_CODEC), ParticleMapping::codec).codec();

    static ParticleMapping ofIdentifier(String identifier) {
        return new IdentifierMapping(Identifier.parse(identifier));
    }

    static ParticleMapping ofType(ParticleType type) {
        return new ParticleTypeMapping(type);
    }

    static ParticleMapping ofEvent(LevelEvent event) {
        return new LevelEventMapping(event);
    }

    static ParticleMapping builtIn() {
        return BuiltInMapping.UNIT;
    }

    MapCodec<? extends ParticleMapping> codec();

    default Optional<Identifier> identifier() {
        return Optional.empty();
    }

    record IdentifierMapping(Identifier bedrockIdentifier) implements ParticleMapping {
        private static final MapCodec<IdentifierMapping> MAP_CODEC = Identifier.CODEC
                .xmap(IdentifierMapping::new, IdentifierMapping::bedrockIdentifier)
                .fieldOf("bedrock_id");

        @Override
        public MapCodec<IdentifierMapping> codec() {
            return MAP_CODEC;
        }

        @Override
        public Optional<Identifier> identifier() {
            return Optional.of(bedrockIdentifier);
        }
    }

    record ParticleTypeMapping(ParticleType type) implements ParticleMapping {
        private static final MapCodec<ParticleTypeMapping> MAP_CODEC = MappingsCodecs.enumCodec(ParticleType::values, ParticleType::name)
                .xmap(ParticleTypeMapping::new, ParticleTypeMapping::type)
                .fieldOf("particle_type");

        @Override
        public MapCodec<ParticleTypeMapping> codec() {
            return MAP_CODEC;
        }
    }

    record LevelEventMapping(LevelEvent event) implements ParticleMapping {
        private static final MapCodec<LevelEventMapping> MAP_CODEC = MappingsCodecs.enumCodec(LevelEvent::values, LevelEvent::name)
                .xmap(LevelEventMapping::new, LevelEventMapping::event)
                .fieldOf("level_event");

        @Override
        public MapCodec<LevelEventMapping> codec() {
            return MAP_CODEC;
        }
    }

    enum BuiltInMapping implements ParticleMapping {
        UNIT;

        private static final MapCodec<BuiltInMapping> MAP_CODEC = MapCodec.unit(UNIT).fieldOf("built_in");

        @Override
        public MapCodec<BuiltInMapping> codec() {
            return MAP_CODEC;
        }
    }
}
