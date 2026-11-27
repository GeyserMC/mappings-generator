package org.geysermc.mappings.definitions.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record BedrockItem(Identifier name, Identifier commandName, Identifier serializationName,
                          int rawId, String serializationId) {
    public static final Codec<BedrockItem> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("name").forGetter(BedrockItem::name),
                    Identifier.CODEC.fieldOf("command_name").forGetter(BedrockItem::commandName),
                    Identifier.CODEC.fieldOf("serialization_name").forGetter(BedrockItem::serializationName),
                    Codec.INT.fieldOf("raw_id").forGetter(BedrockItem::rawId),
                    Codec.STRING.fieldOf("serialization_id").forGetter(BedrockItem::serializationId)
            ).apply(instance, BedrockItem::new)
    );
}
