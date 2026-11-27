package org.geysermc.mappings.definitions.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public record BedrockBlockProperty<T>(String name, Type type, List<T> values) {
    public static final Codec<BedrockBlockProperty<?>> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(BedrockBlockProperty::name),
                    Packed.MAP_CODEC.forGetter(BedrockBlockProperty::pack)
            ).apply(instance, BedrockBlockProperty::new)
    );

    public <E> List<E> encodeValues(DynamicOps<E> ops) {
        return values.stream()
                .map(value -> type.codec().encodeStart(ops, value).getOrThrow())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private BedrockBlockProperty(String name, Packed<?> packed) {
        this(name, packed.type, (List<T>) packed.values);
    }

    private Packed<T> pack() {
        return new Packed<>(type, values);
    }

    private record Packed<T>(Type type, List<T> values) {
        private static final MapCodec<Packed<?>> MAP_CODEC = Type.CODEC.dispatchMap(Packed::type, Packed::codec);

        @SuppressWarnings("unchecked")
        private static <T> MapCodec<Packed<T>> codec(Type type) {
            return type.listCodec().fieldOf("values")
                    .xmap(list -> new Packed<>(type, (List<T>) list),
                            packed -> (List<Object>) packed.values);
        }
    }

    public enum Type implements StringRepresentable {
        BOOL("bool", Codec.BOOL),
        INT("int", Codec.INT),
        STRING("string", Codec.STRING);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final Codec<?> codec;

        <T> Type(String name, Codec<T> codec) {
            this.name = name;
            this.codec = codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @SuppressWarnings("unchecked")
        private <T> Codec<T> codec() {
            return (Codec<T>) codec;
        }

        private <T> Codec<List<T>> listCodec() {
            return this.<T>codec().fieldOf("value").codec().listOf();
        }
    }
}
