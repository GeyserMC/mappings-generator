package org.geysermc.mappings.definitions.protocol;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public record EnumConstant(String name, int id) {
    private static final MapCodec<List<String>> NAMES_CODEC = Codec.STRING.listOf().fieldOf("enum");
    private static final MapCodec<List<Integer>> BINARY_VALUES_CODEC = Codec.INT.listOf().fieldOf("x-enum-binary-value");

    public static final Codec<Set<EnumConstant>> SET_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    NAMES_CODEC.forGetter(decompose(EnumConstant::name)),
                    BINARY_VALUES_CODEC.forGetter(decompose(EnumConstant::id))
            ).apply(instance, EnumConstant::compose)
    );

    public static int findId(Set<EnumConstant> constants, String name) {
        int i = 0;
        for (EnumConstant constant : constants) {
            if (constant.name.equals(name)) {
                return i;
            }
            i++;
        }
        throw new IllegalArgumentException("Enum does not contain constant named " + name);
    }

    private static <T> Function<Set<EnumConstant>, List<T>> decompose(Function<EnumConstant, T> getter) {
        return constants -> constants.stream().map(getter).toList();
    }

    private static Set<EnumConstant> compose(List<String> names, List<Integer> binaryValues) {
        Set<EnumConstant> constants = new ObjectAVLTreeSet<>(Comparator.comparingInt(EnumConstant::id));
        for (int i = 0; i < names.size(); i++) {
            constants.add(new EnumConstant(names.get(i), binaryValues.get(i)));
        }
        return constants;
    }
}
