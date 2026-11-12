package org.geysermc.mappings.definitions.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.geysermc.mappings.definitions.protocol.EnumConstant;
import org.geysermc.mappings.util.MappingsUtil;

import java.util.Set;

public record BedrockMapDecoration(String name, int argb) {
    private static final int DEFAULT_COLOR = ARGB.white(0xFF);

    public BedrockMapDecoration(Holder<MapDecorationType> javaDecoration) {
        this(MappingsUtil.snakeToPascalCase(javaDecoration.unwrapKey().orElseThrow().identifier().getPath()));
    }

    public BedrockMapDecoration(String name) {
        this(name, DEFAULT_COLOR);
    }

    public Serialized serialize(Set<EnumConstant> bedrockDecorations) {
        return new Serialized(EnumConstant.findId(bedrockDecorations, name), argb);
    }

    public record Serialized(int id, int argb) {
        public static final Codec<BedrockMapDecoration.Serialized> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("bedrock_id").forGetter(BedrockMapDecoration.Serialized::id),
                        ExtraCodecs.ARGB_COLOR_CODEC.optionalFieldOf("color", DEFAULT_COLOR).forGetter(BedrockMapDecoration.Serialized::argb)
                ).apply(instance, BedrockMapDecoration.Serialized::new)
        );
    }
}
