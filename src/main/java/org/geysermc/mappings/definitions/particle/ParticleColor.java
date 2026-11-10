package org.geysermc.mappings.definitions.particle;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public sealed interface ParticleColor {
    Codec<ParticleColor> DIRECT_CODEC = Type.CODEC.dispatch(ParticleColor::type, Type::codec);
    Codec<ParticleColor> CODEC = Codec.either(Codec.INT, DIRECT_CODEC)
            .xmap(either -> either.map(Constant::new, Function.identity()), color -> {
                if (color instanceof Constant(int rgb)) {
                    return Either.left(rgb);
                }
                return Either.right(color);
            });

    static ParticleColor constant(int rgb) {
        return new Constant(rgb);
    }

    static ParticleColor random(int... rgbs) {
        return new Random(IntStream.of(rgbs).boxed().toList());
    }

    Type type();

    enum Type implements StringRepresentable {
        CONSTANT("constant", Constant.MAP_CODEC),
        RANDOM("random", Random.MAP_CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends ParticleColor> codec;

        Type(String name, MapCodec<? extends ParticleColor> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends ParticleColor> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    record Constant(int rgb) implements ParticleColor {
        public static final MapCodec<Constant> MAP_CODEC = Codec.INT.fieldOf("value")
                .xmap(Constant::new, Constant::rgb);

        @Override
        public Type type() {
            return Type.CONSTANT;
        }
    }

    record Random(List<Integer> values) implements ParticleColor {
        public static final MapCodec<Random> MAP_CODEC = Codec.INT.listOf().optionalFieldOf("values", List.of())
                .xmap(Random::new, Random::values);

        @Override
        public Type type() {
            return Type.RANDOM;
        }
    }
}
