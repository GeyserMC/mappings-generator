package org.geysermc.mappings.transformer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/// A transformer is a [pure function](https://en.wikipedia.org/wiki/Pure_function). Given an identical argument,
/// a transformer must always return the same output. A transformer may never have side effects.
///
/// Transformers are used across the generator to convert Java data to its Bedrock counterpart. Transformers
/// are listed in the {@link Transformers} class. They can be directly implemented through a lambda, however
/// for larger transformers it is more common to make use of the {@link Factory} class, which has a broad API
/// to construct transformers for many use cases.
///
/// Transformers can be chained using the {@link Transformer#compose(Transformer)} and {@link Transformer#chain(Transformer)}
/// methods. The {@link Transformer.Maybe} interface is a transformer returning an {@link Optional}, and features
/// helper methods for chaining those as well.
///
/// @param <I> the transformer's input
/// @param <O> the transformer's output
/// @see Transformer.Maybe
/// @see Transformer.Factory
/// @see Transformer#of(Builder)
/// @see Transformer#maybe(Maybe.Builder)
/// @see Factory
/// @see Transformers
@FunctionalInterface
public interface Transformer<I, O> extends Function<I, O> {

    /// Combines this transformer and the given transformer by applying the given transformer first, then this one.
    ///
    /// @param before the transformer to apply before this one
    /// @param <MI> the new ("mapped") input
    /// @return the combined transformer
    default <MI> Transformer<MI, O> compose(Transformer<? super MI, ? extends I> before) {
        return input -> apply(before.apply(input));
    }

    /// Shorthand for {@link Transformer#compose(Transformer)}, taking a {@link Builder}.
    ///
    /// @param before the transformer to apply before this one
    /// @param <MI> the new ("mapped") input
    /// @return the combined transformer
    /// @see Transformer#compose(Transformer)
    default <MI> Transformer<MI, O> composeBuilder(Builder<? super MI, ? extends I> before) {
        return compose(of(before));
    }

    /// Combines this transformer and the given transformer by applying the given transformer after this one.
    ///
    /// @param after the transformer to apply after this one
    /// @param <MO> the new ("mapped") output
    /// @return the combined transformer
    default <MO> Transformer<I, MO> chain(Transformer<? super O, ? extends MO> after) {
        return input -> after.apply(apply(input));
    }

    /// Shorthand for {@link Transformer#chain(Transformer)}, taking a {@link Builder}.
    ///
    /// @param after the transformer to apply after this one
    /// @param <MO> the new ("mapped") output
    /// @return the combined transformer
    /// @see Transformer#chain(Transformer)
    default <MO> Transformer<I, MO> chainBuilder(Builder<? super O, ? extends MO> after) {
        return chain(of(after));
    }

    /// @return this transformer turned into a {@link Maybe} instance, by wrapping the output of the transformer in an {@link Optional}
    default Maybe<I, O> maybe() {
        return i -> Optional.of(apply(i));
    }

    /// @param <T> the type of the transformer
    /// @return a transformer that returns its input
    static <T> Transformer<T, T> identity() {
        return t -> t;
    }

    /// Creates a {@link Transformer} using a {@link Factory}. Finish the {@link Factory} using {@link Factory#finish(Transformer)}.
    ///
    /// @param builder the function building the transformer using a {@link Factory}
    /// @param <I> the transformer's input
    /// @param <O> the transformer's output
    /// @return the built transformer
    static <I, O> Transformer<I, O> of(Builder<I, O> builder) {
        return builder.create(new Factory<>());
    }

    /// Creates a {@link Transformer.Maybe} using a {@link Factory}
    ///
    /// @param builder the function building the transformer using a {@link Factory}
    /// @param <I> the transformer's input
    /// @param <O> the transformer's output
    /// @return the built transformer
    static <I, O> Maybe<I, O> maybe(Maybe.Builder<I, O> builder) {
        Factory<I, O, ?, ?, I, O> baseBuilder = new Factory<>();
        builder.build(baseBuilder);
        return baseBuilder.build();
    }

    /// A {@link Creator} is a function building a transformer based on a "builder input".
    ///
    /// @param <BI> the builder input
    /// @param <I> the transformer's input
    /// @param <O> the transformer's output
    @FunctionalInterface
    interface Creator<BI, I, O> {

        /// Creates the transformer.
        ///
        /// @param builderInput the builder's input
        /// @return the built transformer
        Transformer<I, O> create(BI builderInput);

        /// @return this creator as a {@link MaybeCreator} by calling {@link Transformer#maybe()} on the built transformer
        default MaybeCreator<BI, I, O> maybe() {
            return builderInput -> create(builderInput).maybe();
        }
    }

    /// A {@link MaybeCreator} is a function building a {@link Transformer.Maybe} based on a "builder input".
    ///
    /// @param <BI> the builder input
    /// @param <I> the transformer's input
    /// @param <O> the transformer's output
    @FunctionalInterface
    interface MaybeCreator<BI, I, O> {

        /// Creates the transformer.
        ///
        /// @param builderInput the builder's input
        /// @return the built transformer
        Maybe<I, O> create(BI builderInput);
    }

    /// A {@link Builder} is a function building a transformer using a {@link Factory}. A {@link Builder}
    /// must always end with a {@link Factory#finish(Transformer)} call to turn the {@link Factory}
    /// into a {@link Transformer}.
    ///
    /// @param <I> the transformer's input
    /// @param <O> the transformer's output
    @FunctionalInterface
    interface Builder<I, O> extends Creator<Factory<I, O, ?, ?, I, O>, I, O> {}

    /// A {@link Transformer.Maybe} is a transformer returning an {@link Optional}.
    interface Maybe<I, O> extends Transformer<I, Optional<? extends O>> {

        @Override
        default <MI> Maybe<MI, O> compose(Transformer<? super MI, ? extends I> before) {
            return input -> apply(before.apply(input));
        }

        @Override
        default <MI> Maybe<MI, O> composeBuilder(Transformer.Builder<? super MI, ? extends I> before) {
            return compose(of(before));
        }

        default <MI> Maybe<MI, O> maybeCompose(Maybe<? super MI, ? extends I> before) {
            return input -> before.apply(input).flatMap(this);
        }

        default <MI> Maybe<MI, O> maybeComposeBuilder(Builder<? super MI, ? extends I> before) {
            return maybeCompose(Transformer.maybe(before));
        }

        default <MO> Maybe<I, MO> maybeMap(Transformer<? super O, ? extends MO> after) {
            return input -> apply(input).map(after);
        }

        default <MO> Maybe<I, MO> maybeChain(Maybe<? super O, ? extends MO> after) {
            return input -> apply(input).flatMap(after);
        }

        default <MO> Maybe<I, MO> maybeMapBuilder(Transformer.Builder<? super O, ? extends MO> after) {
            return maybeMap(of(after));
        }

        default <MO> Maybe<I, MO> maybeChainBuilder(Builder<? super O, ? extends MO> after) {
            return maybeChain(Transformer.maybe(after));
        }

        default Transformer<I, O> orElse(O fallback) {
            return orElse(_ -> fallback);
        }

        default Transformer<I, O> orElse(Transformer<? super I, ? extends O> fallback) {
            return input -> {
                Optional<? extends O> result = apply(input);
                if (result.isPresent()) {
                    return result.get();
                }
                return fallback.apply(input);
            };
        }

        default Transformer<I, O> orElseThrow() {
            return orElse(_ -> {
                throw new NoSuchElementException("Maybe transformer returned no element, and orElseThrow() was used");
            });
        }

        static <T> Maybe<T, T> identity() {
            return Optional::of;
        }

        static <T> Maybe<T, T> empty() {
            return _ -> Optional.empty();
        }

        @FunctionalInterface
        interface Builder<I, O> {

            void build(Factory<I, O, ?, ?, I, O> factory);
        }
    }

    final class Factory<I, O, MaI, MaO, RI, RO> {
        private final Mappers<I, O, RI, RO> mappers;
        private final Factory<RI, RO, ?, ?, RI, RO> root;
        private final @Nullable List<TransformerWithMappers<?, ?, RI, RO>> transformers;

        private @Nullable Factory<MaI, MaO, MaI, MaO, RI, RO> mark;

        private Factory(Mappers<I, O, RI, RO> mappers, Factory<RI, RO, ?, ?, RI, RO> root, @Nullable Factory<MaI, MaO, MaI, MaO, RI, RO> mark) {
            this.mappers = mappers;
            this.root = root;
            this.mark = mark;
            this.transformers = null;
        }

        @SuppressWarnings("unchecked")
        private Factory() {
            this.mappers = (Mappers<I, O, RI, RO>) Mappers.forRoot();
            this.root = (Factory<RI, RO, ?, ?, RI, RO>) this;
            this.mark = null;
            this.transformers = new ObjectArrayList<>();
        }

        private static <I, O, RI, RO> Factory<I, O, I, O, RI, RO> marked(Factory<I, O, ?, ?, RI, RO> factory) {
            Factory<I, O, I, O, RI, RO> marked = new Factory<>(factory.mappers, factory.root, null);
            // A mark has itself as marker
            marked.mark = marked;
            return marked;
        }

        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> instanceOf(Class<C> clazz, O output) {
            return instanceOf(clazz, _ -> output);
        }

        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> instanceOfBuilder(Class<C> clazz, Builder<C, O> builder) {
            return instanceOf(clazz, of(builder));
        }

        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> instanceOf(Class<C> clazz, Transformer<? super C, ? extends O> transformer) {
            return maybeInstanceOf(clazz, transformer.maybe());
        }

        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> maybeInstanceOfBuilder(Class<C> clazz, Maybe.Builder<C, O> builder) {
            return maybeInstanceOf(clazz, maybe(builder));
        }

        @SuppressWarnings("unchecked")
        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> maybeInstanceOf(Class<C> clazz, Maybe<? super C, ? extends O> transformer) {
            return maybeTransform(i -> clazz.isInstance(i) ? transformer.apply((C) i) : Optional.empty());
        }

        public Factory<I, O, MaI, MaO, RI, RO> tryAll(Transformer<? super I, ? extends O> finalTransformer) {
            return maybeTransform(finalTransformer.maybe());
        }

        @SafeVarargs
        public final Factory<I, O, MaI, MaO, RI, RO> tryAllPermutations(BiFunction<Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>> chainer,
                                                                        Maybe<? super I, ? extends O>... transformers) {
            return tryAllPermutations(chainer, 2, transformers);
        }

        @SafeVarargs
        public final Factory<I, O, MaI, MaO, RI, RO> tryAllPermutations(BiFunction<Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>> chainer, int maxDepth,
                                                                        Maybe<? super I, ? extends O>... transformers) {
            maxDepth = Math.min(transformers.length, maxDepth);
            for (int tryDepth = 0; tryDepth <= maxDepth; tryDepth++) {
                permute(transformers, null, chainer, new boolean[transformers.length], 0, tryDepth);
            }
            return this;
        }

        private void permute(Maybe<? super I, ? extends O>[] transformers,
                             @Nullable Maybe<? super I, ? extends O> prefix,
                             BiFunction<Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>> chainer,
                             boolean[] used, int depth, int tryDepth) {
            for (int i = 0; i < transformers.length; i++) {
                if (used[i]) {
                    continue;
                }
                Maybe<? super I, ? extends O> current = prefix == null ? transformers[i] : chainer.apply(prefix, transformers[i]);
                if (depth >= tryDepth) {
                    maybeTryAll(current);
                } else {
                    used[i] = true;
                    permute(transformers, current, chainer, used, depth + 1, tryDepth);
                    used[i] = false;
                }
            }
        }

        public Factory<I, O, MaI, MaO, RI, RO> maybeTryAll(Maybe<? super I, ? extends O> finalTransformer) {
            return maybeTransform(_ -> true, finalTransformer);
        }

        public Factory<I, O, MaI, MaO, RI, RO> transform(I input, Transformer<? super RI, ? extends RO> transformer) {
            assert root != this : "use transform(I, O) as this is the root";
            root.maybeTransform(rootInput -> mappers.inputMapper.apply(rootInput).map(i -> i.equals(input)).orElse(false) ? Optional.of(transformer.apply(rootInput)) : Optional.empty());
            return this;
        }

        public Factory<I, O, MaI, MaO, RI, RO> transform(I input, O to) {
            if (root != this) {
                // Ensure the output is valid
                mappers.outputMapper.apply(to).orElseThrow(() -> new IllegalStateException("Hardcoded transformation failed to map to root: " + to));
            }
            return transform(Predicate.isEqual(input), to);
        }

        public Factory<I, O, MaI, MaO, RI, RO> transform(Predicate<I> inputPredicate, O to) {
            return transform(inputPredicate, _ -> to);
        }

        public Factory<I, O, MaI, MaO, RI, RO> transformBuilder(Predicate<I> inputPredicate, Builder<I, O> builder) {
            return transform(inputPredicate, of(builder));
        }

        public Factory<I, O, MaI, MaO, RI, RO> transform(Predicate<I> inputPredicate, Transformer<? super I, ? extends O> transformer) {
            return maybeTransform(i -> inputPredicate.test(i) ? Optional.of(transformer.apply(i)) : Optional.empty());
        }

        public Factory<I, O, MaI, MaO, RI, RO> maybeTransformBuilder(Predicate<I> inputPredicate, Maybe.Builder<? super I, ? extends O> builder) {
            return maybeTransform(inputPredicate, maybe(builder));
        }

        public Factory<I, O, MaI, MaO, RI, RO> maybeTransform(Predicate<I> inputPredicate, Maybe<? super I, ? extends O> transformer) {
            return maybeTransform(transformer.maybeCompose(i -> Optional.of(i).filter(inputPredicate)));
        }

        public Factory<I, O, MaI, MaO, RI, RO> maybeTransformBuilder(Maybe.Builder<? super I, ? extends O> builder) {
            return maybeTransform(maybe(builder));
        }

        public Factory<I, O, MaI, MaO, RI, RO> maybeTransform(Maybe<? super I, ? extends O> transformer) {
            assert root.transformers != null;
            root.transformers.add(new TransformerWithMappers<>(mappers, transformer));
            return this;
        }

        public Factory<I, O, MaI, MaO, RI, RO> check(Predicate<? super O> predicate) {
            return flatMapOutput(output -> predicate.test(output) ? Optional.of(output) : Optional.empty());
        }

        public <MI> Factory<MI, O, MaI, MaO, RI, RO> mapInput(Transformer<? super I, ? extends MI> inputMapper) {
            return flatMap(inputMapper.maybe(), Optional::of);
        }

        public <MI> Factory<MI, O, MaI, MaO, RI, RO> flatMapInput(Maybe<? super I, ? extends MI> inputMapper) {
            return flatMap(inputMapper, Optional::of);
        }

        public <MO> Factory<I, MO, MaI, MaO, RI, RO> mapOutput(Transformer<? super MO, ? extends O> outputMapper) {
            return flatMap(Optional::of, outputMapper.maybe());
        }

        public <MO> Factory<I, MO, MaI, MaO, RI, RO> flatMapOutput(Maybe<? super MO, ? extends O> outputMapper) {
            return flatMap(Optional::of, outputMapper);
        }

        public <MI, MO> Factory<MI, MO, MaI, MaO, RI, RO> flatMap(Maybe<? super I, ? extends MI> inputMapper, Maybe<? super MO, ? extends O> outputMapper) {
            return new Factory<>(mappers.map(inputMapper, outputMapper), root, mark);
        }

        public Factory<I, O, I, O, RI, RO> mark() {
            return marked(this);
        }

        public Factory<MaI, MaO, MaI, MaO, RI, RO> reset() {
            assert mark != null : "can only perform reset if mark is set";
            return mark;
        }

        public Factory<RI, RO, ?, ?, RI, RO> root() {
            return root;
        }

        public Transformer<RI, RO> finish(RO fallback) {
            return finish(_ -> fallback);
        }

        public Transformer<RI, RO> finish(Transformer<? super RI, ? extends RO> finalTransformer) {
            List<TransformerWithMappers<?, ?, RI, RO>> transformers = gatherTransformers();
            return input -> {
                Set<Mappers<?, ?, RI, RO>> failedMappers = new ObjectOpenHashSet<>();
                for (TransformerWithMappers<?, ?, RI, RO> transformer : transformers) {
                    if (failedMappers.contains(transformer.mappers)) {
                        continue;
                    }
                    TransformerWithMappers.Result<RO> result = transformer.apply(input);
                    if (result.mappersFailed) {
                        failedMappers.add(transformer.mappers);
                    } else if (result.result.isPresent()) {
                        return result.result.get();
                    }
                }
                return finalTransformer.apply(input);
            };
        }

        public Maybe<RI, RO> build() {
            List<TransformerWithMappers<?, ?, RI, RO>> transformers = gatherTransformers();
            return input -> {
                Set<Mappers<?, ?, RI, RO>> failedMappers = new ObjectOpenHashSet<>();
                for (TransformerWithMappers<?, ?, RI, RO> transformer : transformers) {
                    if (failedMappers.contains(transformer.mappers)) {
                        continue;
                    }
                    TransformerWithMappers.Result<RO> result = transformer.apply(input);
                    if (result.mappersFailed) {
                        failedMappers.add(transformer.mappers);
                    } else if (result.result.isPresent()) {
                        return result.result;
                    }
                }
                return Optional.empty();
            };
        }

        private List<TransformerWithMappers<?, ?, RI, RO>> gatherTransformers() {
            return List.copyOf(Objects.requireNonNull(root().transformers));
        }

        private record Mappers<I, O, RI, RO>(Maybe<? super RI, ? extends I> inputMapper, Maybe<? super O, ? extends RO> outputMapper) {

            private static <RI, RO> Mappers<RI, RO, RI, RO> forRoot() {
                return new Mappers<>(Maybe.identity(), Maybe.identity());
            }

            private <MI, MO> Mappers<MI, MO, RI, RO> map(Maybe<? super I, ? extends MI> inputMapper, Maybe<? super MO, ? extends O> outputMapper) {
                return new Mappers<>(this.inputMapper.maybeChain(inputMapper), outputMapper.maybeChain(this.outputMapper));
            }
        }

        private record TransformerWithMappers<I, O, RI, RO>(Mappers<I, O, RI, RO> mappers, Maybe<? super I, ? extends O> transformer) {

            private Result<RO> apply(RI input) {
                Optional<? extends I> mappedInput = mappers.inputMapper.apply(input);
                if (mappedInput.isEmpty()) {
                    return new Result<>(Optional.empty(), true);
                }
                return new Result<>(mappedInput.flatMap(transformer).flatMap(mappers.outputMapper), false);
            }

            private record Result<RO>(Optional<RO> result, boolean mappersFailed) {}
        }
    }
}
