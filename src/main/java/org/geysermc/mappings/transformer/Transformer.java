package org.geysermc.mappings.transformer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface Transformer<I, O> extends Function<I, O> {

    default <MI> Transformer<MI, O> compose(Transformer<? super MI, ? extends I> before) {
        return input -> apply(before.apply(input));
    }

    default <MI> Transformer<MI, O> composeBuilder(Builder<? super MI, ? extends I> before) {
        return compose(of(before));
    }

    default <MO> Transformer<I, MO> chain(Transformer<? super O, ? extends MO> after) {
        return input -> after.apply(apply(input));
    }

    default <MO> Transformer<I, MO> chainBuilder(Builder<? super O, ? extends MO> after) {
        return chain(of(after));
    }

    default Maybe<I, O> maybe() {
        return i -> Optional.of(apply(i));
    }

    static <T> Transformer<T, T> identity() {
        return t -> t;
    }

    static <I, O> Maybe<I, O> maybe(Maybe.Builder<I, O> builder) {
        Factory<I, O, ?, ?, I, O> baseBuilder = new Factory<>();
        builder.build(baseBuilder);
        return baseBuilder.build();
    }

    static <I, O> Transformer<I, O> of(Builder<I, O> builder) {
        return builder.build(new Factory<>());
    }

    @FunctionalInterface
    interface Creator<BI, I, O> {

        Transformer<I, O> create(BI builderInput);
    }

    @FunctionalInterface
    interface Builder<I, O> {

        Transformer<I, O> build(Factory<I, O, ?, ?, I, O> factory);
    }

    @FunctionalInterface
    interface MaybeCreator<BI, I, O> {

        Maybe<I, O> create(BI builderInput);
    }

    interface Maybe<I, O> extends Transformer<I, Optional<? extends O>> {

        @Override
        default <MI> Maybe<MI, O> compose(Transformer<? super MI, ? extends I> before) {
            return input -> apply(before.apply(input));
        }

        @Override
        default <MI> Transformer<MI, Optional<? extends O>> composeBuilder(Transformer.Builder<? super MI, ? extends I> before) {
            return compose(of(before));
        }

        default <MI> Maybe<MI, O> maybeCompose(Maybe<? super MI, ? extends I> before) {
            return input -> before.apply(input).flatMap(this);
        }

        default <MI> Maybe<MI, O> maybeComposeBuilder(Builder<? super MI, ? extends I> before) {
            return maybeCompose(Transformer.maybe(before));
        }

        default <MO> Maybe<I, MO> maybeChain(Maybe<? super O, ? extends MO> after) {
            return input -> apply(input).flatMap(after);
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

        @FunctionalInterface
        interface Builder<I, O> {

            void build(Factory<I, O, ?, ?, I, O> factory);
        }
    }

    final class Factory<I, O, MaI, MaO, RI, RO> {
        private final @Nullable Root<I, O, RI, RO> root;
        private final @Nullable List<Maybe<? super I, ? extends O>> transformers;

        private @Nullable Factory<MaI, MaO, MaI, MaO, RI, RO> mark;

        private Factory(@Nullable Root<I, O, RI, RO> root, @Nullable Factory<MaI, MaO, MaI, MaO, RI, RO> mark) {
            this.root = root;
            this.mark = mark;
            this.transformers = null;
        }

        private Factory() {
            this.root = null;
            this.mark = null;
            this.transformers = new ObjectArrayList<>();
        }

        private static <I, O, RI, RO> Factory<I, O, I, O, RI, RO> marked(Factory<I, O, ?, ?, RI, RO> factory) {
            Factory<I, O, I, O, RI, RO> marked = new Factory<>(factory.root, null);
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

        @SafeVarargs
        public final Factory<I, O, MaI, MaO, RI, RO> groupedCatch(BiFunction<Transformer<? super I, ? extends O>, Transformer<? super I, ? extends O>, Transformer<? super I, ? extends O>> chainer,
                                                                  Transformer<? super I, ? extends O>... transformers) {
            for (Transformer<? super I, ? extends O> first : transformers) {
                Transformer<? super I, ? extends O> current = first;
                for (Transformer<? super I, ? extends O> second : transformers) {
                    if (first == second) {
                        continue;
                    }
                    current = chainer.apply(current, second);
                    catchAll(current);
                }
                catchAll(first);
            }
            return this;
        }

        public Factory<I, O, MaI, MaO, RI, RO> catchAll(Transformer<? super I, ? extends O> finalTransformer) {
            return transform(_ -> true, finalTransformer);
        }

        public Factory<I, O, MaI, MaO, RI, RO> transform(I input, Transformer<? super RI, ? extends RO> transformer) {
            assert root != null : "use transform(I, O) as this is the root";
            root.root.maybeTransform(rootInput -> root.inputMapper.apply(rootInput).map(i -> i.equals(input)).orElse(false) ? Optional.of(transformer.apply(rootInput)) : Optional.empty());
            return this;
        }

        public Factory<I, O, MaI, MaO, RI, RO> transform(I input, O to) {
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
            if (root != null) {
                root.delegate(transformer);
            } else {
                assert transformers != null;
                transformers.add(transformer);
            }
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

        @SuppressWarnings("unchecked")
        public <MI, MO> Factory<MI, MO, MaI, MaO, RI, RO> flatMap(Maybe<? super I, ? extends MI> inputMapper, Maybe<? super MO, ? extends O> outputMapper) {
            Root<MI, MO, RI, RO> mappedRoot;
            if (root != null) {
                mappedRoot = root.map(inputMapper, outputMapper);
            } else {
                mappedRoot = new Root<>(root(),
                        (Maybe<? super RI, ? extends MI>) inputMapper,
                        (Maybe<? super MO, ? extends RO>) outputMapper);
            }
            return new Factory<>(mappedRoot, mark);
        }

        public Factory<I, O, I, O, RI, RO> mark() {
            return marked(this);
        }

        public Factory<MaI, MaO, MaI, MaO, RI, RO> reset() {
            assert mark != null : "can only perform reset if mark is set";
            return mark;
        }

        @SuppressWarnings("unchecked")
        public Factory<RI, RO, ?, ?, RI, RO> root() {
            if (root != null) {
                return root.root;
            }
            return (Factory<RI, RO, ?, ?, RI, RO>) this;
        }

        public Transformer<RI, RO> finish(RO fallback) {
            return finish(_ -> fallback);
        }

        public Transformer<RI, RO> finish(Transformer<? super RI, ? extends RO> finalTransformer) {
            List<Maybe<? super RI, ? extends RO>> transformers = gatherTransformers();
            return input -> {
                for (Maybe<? super RI, ? extends RO> transformer : transformers) {
                    Optional<? extends RO> result = transformer.apply(input);
                    if (result.isPresent()) {
                        return result.get();
                    }
                }
                return finalTransformer.apply(input);
            };
        }

        public Maybe<RI, RO> build() {
            List<Maybe<? super RI, ? extends RO>> transformers = gatherTransformers();
            return input -> {
                for (Maybe<? super RI, ? extends RO> transformer : transformers) {
                    Optional<? extends RO> result = transformer.apply(input);
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return Optional.empty();
            };
        }

        @SuppressWarnings("unchecked")
        private Optional<? extends I> mapInput(RI input) {
            if (root == null) {
                return Optional.of((I) input);
            }
            return root.inputMapper.apply(input);
        }

        @SuppressWarnings("unchecked")
        private Optional<? extends RO> mapOutput(O output) {
            if (root == null) {
                return Optional.of((RO) output);
            }
            return root.outputMapper.apply(output);
        }

        private List<Maybe<? super RI, ? extends RO>> gatherTransformers() {
            return List.copyOf(Objects.requireNonNull(root().transformers));
        }

        private record Root<I, O, RI, RO>(Factory<RI, RO, ?, ?, RI, RO> root,
                                          Maybe<? super RI, ? extends I> inputMapper,
                                          Maybe<? super O, ? extends RO> outputMapper) {

            private <MI, MO> Root<MI, MO, RI, RO> map(Maybe<? super I, ? extends MI> inputMapper, Maybe<? super MO, ? extends O> outputMapper) {
                return new Root<>(root, this.inputMapper.maybeChain(inputMapper), outputMapper.maybeChain(this.outputMapper));
            }

            private void delegate(Maybe<? super I, ? extends O> transformer) {
                root.maybeTransform(transformer.maybeCompose(inputMapper).maybeChain(outputMapper));
            }
        }
    }
}
