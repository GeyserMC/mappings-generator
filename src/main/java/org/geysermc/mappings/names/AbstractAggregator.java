package org.geysermc.mappings.names;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

abstract class AbstractAggregator<T, I, R> implements InstanceRenamer.Aggregator<T, I, R, T, I, R> {
    protected final Map<T, Function<I, R>> renamers = new Object2ObjectOpenHashMap<>();

    @Override
    public <M> InstanceRenamer.Aggregator<M, I, R, T, I, R> mapType(Function<M, T> mapper) {
        return new Delegate<>(this, mapper, Function.identity(), Function.identity());
    }

    @Override
    public <M> InstanceRenamer.Aggregator<T, M, R, T, I, R> mapInstance(Function<I, M> mapper) {
        return new Delegate<>(this, Function.identity(), mapper, Function.identity());
    }

    @Override
    public <M> InstanceRenamer.Aggregator<T, I, M, T, I, R> mapResult(Function<M, R> mapper) {
        return new Delegate<>(this, Function.identity(), Function.identity(), mapper);
    }

    @Override
    public InstanceRenamer.Aggregator<T, I, R, T, I, R> rename(T type, Function<I, R> renamer) {
        if (renamers.containsKey(type)) {
            throw new IllegalArgumentException("Already registered a renamer for " + type);
        }
        renamers.put(type, renamer);
        return this;
    }

    static class Builder<T, I, R> extends AbstractAggregator<T, I, R> {
        protected final BiFunction<T, I, R> fallback;

        Builder(BiFunction<T, I, R> fallback) {
            this.fallback = fallback;
        }

        public InstanceRenamer<T, I, R> build() {
            return type -> renamers.getOrDefault(type, instance -> fallback.apply(type, instance));
        }
    }

    static class OptionalBuilder<T, I, R> extends AbstractAggregator<T, I, R> {

        public InstanceRenamer<T, I, Optional<R>> build() {
            return type -> Optional.ofNullable(renamers.get(type))
                    .map(renamer -> renamer.andThen(Optional::of))
                    .orElse(_ -> Optional.empty());
        }
    }

    static class TypeOnly<T, R> extends Builder<T, T, R> {

        public TypeOnly(Function<T, R> fallback) {
            super((type, _) -> fallback.apply(type));
        }

        @Override
        public TypeRenamer<T, R> build() {
            return type -> renamers.getOrDefault(type, instance -> fallback.apply(type, instance));
        }
    }

    static class TypeOnlyOptional<T, R> extends OptionalBuilder<T, T, R> {

        @Override
        public TypeRenamer<T, Optional<R>> build() {
            return type -> Optional.ofNullable(renamers.get(type))
                    .map(renamer -> renamer.andThen(Optional::of))
                    .orElse(_ -> Optional.empty());
        }
    }

    private record Delegate<T, I, R, BT, BI, BR>(AbstractAggregator<BT, BI, BR> base,
                                                 Function<T, BT> typeToBase,
                                                 Function<BI, I> baseToInstance,
                                                 Function<R, BR> resultToBase) implements InstanceRenamer.Aggregator<T, I, R, BT, BI, BR> {

        @Override
        public <M> InstanceRenamer.Aggregator<M, I, R, BT, BI, BR> mapType(Function<M, BT> mapper) {
            return new Delegate<>(base, mapper, baseToInstance, resultToBase);
        }

        @Override
        public <M> InstanceRenamer.Aggregator<T, M, R, BT, BI, BR> mapInstance(Function<BI, M> mapper) {
            return new Delegate<>(base, typeToBase, mapper, resultToBase);
        }

        @Override
        public <M> InstanceRenamer.Aggregator<T, I, M, BT, BI, BR> mapResult(Function<M, BR> mapper) {
            return new Delegate<>(base, typeToBase, baseToInstance, mapper);
        }

        @Override
        public InstanceRenamer.Aggregator<T, I, R, BT, BI, BR> rename(T type, Function<I, R> renamer) {
            base.rename(typeToBase.apply(type), renamer.compose(baseToInstance).andThen(resultToBase));
            return this;
        }
    }
}
