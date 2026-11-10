package org.geysermc.mappings.definitions.particle;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public record ParticleColor(String variable, Provider provider) {
    public static final String DEFAULT_COLOR_VARIABLE = "variable.color";

    public static final Codec<ParticleColor> FULL_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("variable", DEFAULT_COLOR_VARIABLE).forGetter(ParticleColor::variable),
                    Provider.MAP_CODEC.forGetter(ParticleColor::provider)
            ).apply(instance, ParticleColor::new)
    );
    public static final Codec<ParticleColor> CODEC = Codec.either(Codec.INT, FULL_CODEC)
            .xmap(either -> either.map(ParticleColor::constant, Function.identity()), color -> {
                if (color.variable.equals(DEFAULT_COLOR_VARIABLE) && color.provider instanceof Provider.Constant(int rgb)) {
                    return Either.left(rgb);
                }
                return Either.right(color);
            });

    public static ParticleColor constant(int rgb) {
        return new ParticleColor(DEFAULT_COLOR_VARIABLE, new Provider.Constant(rgb));
    }

    public static ParticleColor random(int... rgbs) {
        return random(DEFAULT_COLOR_VARIABLE, rgbs);
    }

    public static ParticleColor random(String variable, int... rgbs) {
        return new ParticleColor(variable, new Provider.Random(IntStream.of(rgbs).boxed().toList()));
    }

    public sealed interface Provider {
        MapCodec<Provider> MAP_CODEC = Type.CODEC.dispatchMap(Provider::type, Type::codec);

        Type type();

        enum Type implements StringRepresentable {
            CONSTANT("constant", Constant.MAP_CODEC),
            RANDOM("random", Random.MAP_CODEC);

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

            private final String name;
            private final MapCodec<? extends Provider> codec;

            Type(String name, MapCodec<? extends Provider> codec) {
                this.name = name;
                this.codec = codec;
            }

            public MapCodec<? extends Provider> codec() {
                return codec;
            }

            @Override
            public String getSerializedName() {
                return name;
            }
        }

        record Constant(int rgb) implements Provider {
            public static final MapCodec<Constant> MAP_CODEC = Codec.INT.fieldOf("value")
                    .xmap(Constant::new, Constant::rgb);

            @Override
            public Type type() {
                return Type.CONSTANT;
            }
        }

        record Random(List<Integer> values) implements Provider {
            public static final MapCodec<Random> MAP_CODEC = Codec.INT.listOf().optionalFieldOf("values", List.of())
                    .xmap(Random::new, Random::values);

            @Override
            public Type type() {
                return Type.RANDOM;
            }
        }
    }

}
