package org.geysermc.mappings.names;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/// An {@link InstanceRenamer} (and by extension, its sibling, {@link TypeRenamer}) is used to easily register a map of Java -> bedrock names (or vice versa).
///
/// An {@link InstanceRenamer} takes a type {@link T} (e.g., {@link Block}), and returns a function mapping an in-game instance of {@link T},
/// {@link I} (e.g. {@link BlockState}), to a result {@link R} (usually a {@link String} or {@link Identifier}).
///
/// Important to note is that instances {@link I} are not necessarily actually instances of {@link T}, they are just related to each other in some way.
///
/// Helper methods to create {@link InstanceRenamer}s exist, and generally you'll want to use these. They allow registering a number of "renames" for different types {@link T},
/// with a fallback function that is used when no "rename" is registered.
///
/// If your renamer always returns the same names, no matter the instance {@link I}, you should use {@link TypeRenamer} instead.
///
/// {@link InstanceRenamer}s and {@link TypeRenamer}s may be hard to grasp at first, however you should very quickly feel familiar with them after looking at some examples in
/// {@link Renamers} or {@link org.geysermc.mappings.names.renamers}.
///
/// @param <T> the type to be renamed, e.g. {@link Block}
/// @param <I> the instance of the type {@link T} in-game, e.g. {@link BlockState}
/// @param <R> the rename result, e.g. {@link String} or {@link Identifier}
/// @see TypeRenamer
/// @see Aggregator
/// @see InstanceRenamer#of(Function, Consumer)
/// @see InstanceRenamer#of(BiFunction, Consumer)
@FunctionalInterface
public interface InstanceRenamer<T, I, R> {

    Function<I, R> forType(T type);

    /// Creates a new {@link InstanceRenamer} by using an "aggregated" set of renames and a fallback function. Callers should specify a fallback function,
    /// which takes an instance {@link I} (e.g. {@link BlockState}), and turns it into its rename {@link R}, and an {@link Aggregator} consumer, which "aggregates"
    /// the set of rename overrides, for which the fallback function won't suffice.
    ///
    /// If the fallback function requires the type {@link T} (e.g. {@link Block}) as well as the instance {@link I}, use {@link InstanceRenamer#of(BiFunction, Consumer)} instead.
    ///
    /// @param fallback a function that takes an instance {@link I}, and turns it into its rename {@link R}
    /// @param builder a consumer that takes an {@link Aggregator}, and registers renames to it
    /// @param <T> the type to be renamed, e.g. {@link Block}
    /// @param <I> the instance of the type {@link T} in-game, e.g. {@link BlockState}
    /// @param <R> the rename result, e.g. {@link String} or {@link Identifier}
    /// @return the created {@link InstanceRenamer}
    static <T, I, R> InstanceRenamer<T, I, R> of(Function<I, R> fallback, Consumer<Aggregator<T, I, R>> builder) {
        return of((_, instance) -> fallback.apply(instance), builder);
    }

    /// Creates a new {@link InstanceRenamer} by using an "aggregated" set of renames and a fallback function. Callers should specify a fallback function,
    /// which takes a type {@link T} (e.g. {@link Block}), and an instance {@link I} (e.g. {@link BlockState}),
    /// and turns it into its rename {@link R}, and an {@link Aggregator} consumer, which "aggregates"
    /// the set of rename overrides, for which the fallback function won't suffice.
    ///
    /// If the fallback function does not require the type {@link T}, but only the instance {@link I}, use {@link InstanceRenamer#of(Function, Consumer)} instead.
    ///
    /// @param fallback a function that takes a type {@link T} and an instance {@link I}, and turns it into its rename {@link R}
    /// @param builder a consumer that takes an {@link Aggregator}, and registers renames to it
    /// @param <T> the type to be renamed, e.g. {@link Block}
    /// @param <I> the instance of the type {@link T} in-game, e.g. {@link BlockState}
    /// @param <R> the rename result, e.g. {@link String} or {@link Identifier}
    /// @return the created {@link InstanceRenamer}
    static <T, I, R> InstanceRenamer<T, I, R> of(BiFunction<T, I, R> fallback, Consumer<Aggregator<T, I, R>> builder) {
        Builder<T, I, R> renamer = new Builder<>(fallback);
        builder.accept(renamer);
        return renamer.build();
    }

    /// An {@link Aggregator} "aggregates", or collects, rename overrides for an {@link InstanceRenamer} or {@link TypeRenamer}.
    @FunctionalInterface
    interface Aggregator<T, I, R> {

        /// Returns a new {@link Aggregator} which delegates back to this {@link Aggregator}, with its type {@link T} mapped to
        /// {@link M}, using the given `mapper` function.
        ///
        /// This can be useful when your type {@link T} is an {@link Identifier} for example, but you want to pass a {@link ResourceKey}:
        ///
        /// ```java
        /// public static final TypeRenamer<Identifier, String> BIOMES = TypeRenamer.of(Identifier::getPath, builder -> builder
        ///         .<ResourceKey<Biome>>mapType(ResourceKey::identifier)
        ///         .rename(Biomes.BADLANDS, "mesa") // Automatically turns Biomes.BADLANDS into Identifier["minecraft:badlands"]
        ///         .rename(Biomes.ERODED_BADLANDS, "mesa_bryce"));
        /// ```
        default <M> Aggregator<M, I, R> mapType(Function<M, T> mapper) {
            return new Aggregator<>() {
                @Override
                public Aggregator<M, I, R> rename(M type, Function<I, R> renamer) {
                    Aggregator.this.rename(mapper.apply(type), renamer);
                    return this;
                }
            };
        }

        /// Returns a new {@link Aggregator} which delegates back to this {@link Aggregator}, with its instance {@link I} mapped to
        /// {@link M}, using the given `mapper` function.
        default <M> Aggregator<T, M, R> mapInstance(Function<I, M> mapper) {
            return new Aggregator<>() {
                @Override
                public Aggregator<T, M, R> rename(T type, Function<M, R> renamer) {
                    Aggregator.this.rename(type, mapper.andThen(renamer));
                    return this;
                }
            };
        }

        /// Returns a new {@link Aggregator} which delegates back to this {@link Aggregator}, with its result {@link R} mapped to
        /// {@link M}, using the given `mapper` function.
        ///
        /// This can be useful when your result {@link R} is an {@link Identifier} for example, but you want to pass a {@link String} and automatically add a vanilla
        /// namespace:
        ///
        /// ```java
        /// public static final InstanceRenamer<Item, Identifier, Identifier> ITEMS = InstanceRenamer.of(ItemNames::mapItemIdentifier, builder -> builder
        ///         .mapResult(Identifier::withDefaultNamespace)
        ///         .rename(Items.MAP, "empty_map") // Automatically turns "empty_map" into Identifier["minecraft:empty_map"]
        /// ```
        default <M> Aggregator<T, I, M> mapResult(Function<M, R> mapper) {
            return new Aggregator<>() {
                @Override
                public Aggregator<T, I, M> rename(T type, Function<I, M> renamer) {
                    Aggregator.this.rename(type, renamer.andThen(mapper));
                    return this;
                }
            };
        }

        default DefaultedAggregator<T, I, R> withIdenticalNamer(Function<T, R> identicalNamer) {
            return new DefaultedAggregator<>() {
                @Override
                public R identicalName(T type) {
                    return identicalNamer.apply(type);
                }

                @Override
                public DefaultedAggregator<T, I, R> rename(T type, Function<I, R> renamer) {
                    Aggregator.this.rename(type, renamer);
                    return this;
                }
            };
        }

        /// Registers a constant rename for the given type {@link T}.
        /// Use this method if the result for the type is always the same, no matter the instance {@link I}.
        ///
        /// @param type the type {@link T} to register the rename for
        /// @param to what to rename the type {@link T} to
        default Aggregator<T, I, R> rename(T type, R to) {
            return rename(type, _ -> to);
        }

        /// Registers a renamer for the given type {@link T}.
        ///
        /// @param type the type {@link T} to register the renamer for
        /// @param renamer the renamer function, which takes an instance {@link I} and turns it into a result {@link R}
        Aggregator<T, I, R> rename(T type, Function<I, R> renamer);
    }

    interface DefaultedAggregator<T, I, R> extends Aggregator<T, I, R> {

        R identicalName(T type);

        default DefaultedAggregator<T, I, R> identical(T type) {
            return rename(type, identicalName(type));
        }

        @Override
        default DefaultedAggregator<T, I, R> rename(T type, R to) {
            return (DefaultedAggregator<T, I, R>) Aggregator.super.rename(type, to);
        }

        @Override
        DefaultedAggregator<T, I, R> rename(T type, Function<I, R> renamer);
    }

    class Builder<T, I, R> implements Aggregator<T, I, R> {
        protected final BiFunction<T, I, R> fallback;
        protected final Map<T, Function<I, R>> renamers = new Object2ObjectOpenHashMap<>();

        public Builder(BiFunction<T, I, R> fallback) {
            this.fallback = fallback;
        }

        @Override
        public Builder<T, I, R> rename(T type, Function<I, R> renamer) {
            if (renamers.containsKey(type)) {
                throw new IllegalArgumentException("Already registered a renamer for " + type);
            }
            renamers.put(type, renamer);
            return this;
        }

        public InstanceRenamer<T, I, R> build() {
            return type -> renamers.getOrDefault(type, instance -> fallback.apply(type, instance));
        }
    }
}
