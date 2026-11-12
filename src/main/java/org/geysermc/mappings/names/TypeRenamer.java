package org.geysermc.mappings.names;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/// A {@link TypeRenamer} is a simplified {@link InstanceRenamer}, to be used when all renames are "instance-independent".
///
/// @see InstanceRenamer
@FunctionalInterface
public interface TypeRenamer<T, R> extends InstanceRenamer<T, T, R> {

    default R get(T type) {
        return forType(type).apply(type);
    }

    static <T, R> TypeRenamer<T, R> of(Function<T, R> fallback, Function<Aggregator<T, T, R, T, T, R>, ?> builder) {
        AbstractAggregator.TypeOnly<T, R> renamer = new AbstractAggregator.TypeOnly<>(fallback);
        builder.apply(renamer);
        return renamer.build();
    }

    static <T, R> TypeRenamer<T, Optional<R>> ofOptional(Function<Aggregator<T, T, R, T, T, R>, ?> builder) {
        AbstractAggregator.TypeOnlyOptional<T, R> renamer = new AbstractAggregator.TypeOnlyOptional<>();
        builder.apply(renamer);
        return renamer.build();
    }
}
