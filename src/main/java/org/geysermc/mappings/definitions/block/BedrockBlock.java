package org.geysermc.mappings.definitions.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record BedrockBlock(Identifier name, List<BedrockBlockProperty<?>> properties,
                           int rawId, String serializationId,
                           List<CompoundTag> states) {

    public BedrockBlock(Identifier name, List<BedrockBlockProperty<?>> properties,
                        int rawId, String serializationId) {
        this(name, properties, rawId, serializationId, createStates(properties));
    }

    private static List<CompoundTag> createStates(List<BedrockBlockProperty<?>> properties) {
        if (properties.isEmpty()) {
            return List.of();
        }

        Stream<List<Property>> states = Stream.of(List.of());
        for (BedrockBlockProperty<?> newProperty : properties) {
            states = states.flatMap(currentProperties -> newProperty.encodeValues(NbtOps.INSTANCE).stream().map(tag -> {
                List<Property> combinedProperties = new ArrayList<>(currentProperties);
                combinedProperties.add(new Property(newProperty.name(), tag));
                return combinedProperties;
            }));
        }

        return states.map(state -> {
            CompoundTag tag = new CompoundTag();
            state.forEach(property -> tag.put(property.name, property.value));
            return tag;
        }).toList();
    }

    private record Property(String name, Tag value) {}

    public static Codec<BedrockBlock> codec(Map<String, BedrockBlockProperty<?>> properties) {
        Codec<BedrockBlockProperty<?>> propertyCodec = Codec.stringResolver(BedrockBlockProperty::name, properties::get).fieldOf("name").codec();
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        Identifier.CODEC.fieldOf("name").forGetter(BedrockBlock::name),
                        propertyCodec.listOf().fieldOf("properties").forGetter(BedrockBlock::properties),
                        Codec.INT.fieldOf("raw_id").forGetter(BedrockBlock::rawId),
                        Codec.STRING.fieldOf("serialization_id").forGetter(BedrockBlock::serializationId)
                ).apply(instance, BedrockBlock::new)
        );
    }
}
