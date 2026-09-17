package org.geysermc.mappings.definitions.component;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record ResolvableDataComponentType<T>(DataComponentType<T> type, Function<T, Optional<ResolvableDataComponent>> dataGetter) {
    // Components which do not allow direct serialisation over network and instead are only ever encoded using network IDs
    // Can't automatically detect this until we have mixins
    private static final List<DataComponentType<?>> WALL_OF_SHAME = List.of(DataComponents.CHICKEN_VARIANT, DataComponents.DAMAGE_TYPE,
            DataComponents.BLOCK_TRANSFORMER, DataComponents.PROVIDES_POTTERY_PATTERN);

    public static <T, R extends ResolvableDataComponent> ResolvableDataComponentType<T> of(DataComponentType<T> type, Function<T, R> dataGetter) {
        return new ResolvableDataComponentType<>(type, dataGetter.andThen(Optional::of));
    }

    public static <T, R> ResolvableDataComponentType<T> ofHolder(DataComponentType<T> type, Function<T, Holder<R>> toHolder, boolean allowsDirectOverNetwork) {
        return new ResolvableDataComponentType<>(type, value -> {
            Holder<R> holder = toHolder.apply(value);
            if (holder instanceof Holder.Reference<R> reference) {
                return Optional.of(new HolderReferenceComponent(reference, allowsDirectOverNetwork));
            }
            // Direct holders are encoded, well, directly, and loaded correctly by the base default component loader
            return Optional.empty();
        });
    }

    public static <T> ResolvableDataComponentType<Holder<T>> ofHolder(DataComponentType<Holder<T>> type) {
        return ofHolder(type, Function.identity(), !WALL_OF_SHAME.contains(type));
    }

    public static <T, R> ResolvableDataComponentType<T> ofHolderSet(DataComponentType<T> type, Function<T, HolderSet<R>> toHolderSet) {
        return new ResolvableDataComponentType<>(type, value -> {
            HolderSet<R> holders = toHolderSet.apply(value);
            if (holders instanceof HolderSet.Direct<R> direct) {
                List<Holder<R>> contents = direct.unwrap().right().orElseThrow();
                if (contents.isEmpty()) {
                    return Optional.of(new HolderSetComponent(Optional.empty(), List.of()));
                }
                List<Holder.Reference<R>> references = contents.stream()
                        // We only support references here. If there's any occassion where a holder is not a reference, then, we'll have to implement a fix!
                        .map(holder -> (Holder.Reference<R>) holder)
                        .toList();
                return Optional.of(new HolderSetComponent(Optional.of(references.getFirst().key().registry()), references.stream().map(reference -> reference.key().identifier()).toList()));
            }
            // Named holder sets are tags which the base default component loader loads correctly
            return Optional.empty();
        });
    }

    public static <T> ResolvableDataComponentType<HolderSet<T>> ofHolderSet(DataComponentType<HolderSet<T>> type) {
        return ofHolderSet(type, Function.identity());
    }

    public Optional<TypedResolvableDataComponent> get(DataComponentMap components) {
        T value = components.get(type);
        if (value == null) {
            return Optional.empty();
        }
        return dataGetter.apply(value).map(data -> new TypedResolvableDataComponent(type, data));
    }
}
