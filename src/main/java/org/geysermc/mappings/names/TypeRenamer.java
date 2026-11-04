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

    static <T, R> TypeRenamer<T, R> of(Function<T, R> fallback, Consumer<Aggregator<T, T, R>> builder) {
        Builder<T, R> renamer = new Builder<>(fallback);
        builder.accept(renamer);
        return renamer.build();
    }

    static <T, R> TypeRenamer<T, Optional<R>> ofOptional(Consumer<Aggregator<T, T, R>> builder) {
        Builder<T, Optional<R>> renamer = new Builder<>(_ -> Optional.empty());
        builder.accept(renamer.mapResult(Optional::of));
        return renamer.build();
    }

    class Builder<T, R> extends InstanceRenamer.Builder<T, T, R> {

        public Builder(Function<T, R> fallback) {
            super((type, _) -> fallback.apply(type));
        }

        @Override
        public TypeRenamer<T, R> build() {
            return type -> renamers.getOrDefault(type, instance -> fallback.apply(type, instance));
        }
    }
}
