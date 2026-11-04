package org.geysermc.mappings.definitions.particle;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.geysermc.mappings.util.MappingsCodecs;

import java.util.Optional;

public record ParticleMapping(Either<Identifier, Either<ParticleType, LevelEvent>> bedrockMapping) {
    private static final Codec<ParticleType> PARTICLE_TYPE_CODEC = MappingsCodecs.enumCodec(ParticleType::values, ParticleType::name);
    private static final Codec<LevelEvent> LEVEL_EVENT_CODEC = MappingsCodecs.enumCodec(LevelEvent::values, LevelEvent::name);

    public static final Codec<ParticleMapping> CODEC = Codec.either(Identifier.CODEC.fieldOf("bedrock_id").codec(),
                    Codec.either(PARTICLE_TYPE_CODEC.fieldOf("particle_type").codec(), LEVEL_EVENT_CODEC.fieldOf("level_event").codec()))
            .xmap(ParticleMapping::new, ParticleMapping::bedrockMapping);

    public static ParticleMapping ofIdentifier(String identifier) {
        return new ParticleMapping(Either.left(Identifier.parse(identifier)));
    }

    public static ParticleMapping ofType(ParticleType type) {
        return new ParticleMapping(Either.right(Either.left(type)));
    }

    public static ParticleMapping ofEvent(LevelEvent event) {
        return new ParticleMapping(Either.right(Either.right(event)));
    }

    public Optional<Identifier> identifier() {
        return bedrockMapping.left();
    }
}
