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
        return newInput -> apply(before.apply(newInput));
    }

    default <MO> Transformer<I, MO> chain(Transformer<? super O, ? extends MO> after) {
        return input -> after.apply(apply(input));
    }

    default Transformer<I, O> optionalChain(Transformer<? super O, Optional<? extends O>> after) {
        return chain(output -> {
            Optional<? extends O> mapped = after.apply(output);
            if (mapped.isPresent()) {
                return mapped.get();
            }
            return output;
        });
    }

    default <MO> Transformer<I, MO> strongChainBuilder(StrongBuilder<O, MO> after) {
        return chain(strong(after));
    }

    default Transformer<I, O> optionalChainBuilder(WeakBuilder<O, O> after) {
        return optionalChain(weak(after));
    }

    static <T> Transformer<T, T> identity() {
        return t -> t;
    }

    static <I, O> Transformer<I, Optional<? extends O>> weak(WeakBuilder<I, O> builder) {
        Builder<I, O, ?, ?, I, O> baseBuilder = new Builder<>();
        builder.build(baseBuilder);
        return baseBuilder.build();
    }

    static <I, O> Transformer<I, O> strong(StrongBuilder<I, O> builder) {
        return builder.build(new Builder<>());
    }

    @FunctionalInterface
    interface Creator<BI, I, O> {

        Transformer<I, O> create(BI builderInput);
    }

    @FunctionalInterface
    interface WeakBuilder<I, O> {

        void build(Builder<I, O, ?, ?, I, O> builder);
    }

    @FunctionalInterface
    interface StrongBuilder<I, O> {

        Transformer<I, O> build(Builder<I, O, ?, ?, I, O> builder);
    }

    final class Builder<I, O, MaI, MaO, RI, RO> {
        private final @Nullable Root<I, O, RI, RO> root;
        private final @Nullable List<Transformer<? super I, Optional<? extends O>>> transformers;

        private @Nullable Builder<MaI, MaO, MaI, MaO, RI, RO> mark;

        private Builder(@Nullable Root<I, O, RI, RO> root, @Nullable Builder<MaI, MaO, MaI, MaO, RI, RO> mark) {
            this.root = root;
            this.mark = mark;
            this.transformers = null;
        }

        private Builder() {
            this.root = null;
            this.mark = null;
            this.transformers = new ObjectArrayList<>();
        }

        private static <I, O, RI, RO> Builder<I, O, I, O, RI, RO> marked(Builder<I, O, ?, ?, RI, RO> builder) {
            Builder<I, O, I, O, RI, RO> marked = new Builder<>(builder.root, null);
            // A mark has itself as marker
            marked.mark = marked;
            return marked;
        }

        public <C extends I> Builder<I, O, MaI, MaO, RI, RO> instanceOf(Class<C> clazz, O output) {
            return instanceOf(clazz, _ -> output);
        }

        public <C extends I> Builder<I, O, MaI, MaO, RI, RO> instanceOfWeak(Class<C> clazz, WeakBuilder<C, O> builder) {
            return instanceOfOptional(clazz, weak(builder));
        }

        public <C extends I> Builder<I, O, MaI, MaO, RI, RO> instanceOfStrong(Class<C> clazz, StrongBuilder<C, O> builder) {
            return instanceOf(clazz, strong(builder));
        }

        public <C extends I> Builder<I, O, MaI, MaO, RI, RO> instanceOf(Class<C> clazz, Transformer<? super C, ? extends O> transformer) {
            return instanceOfOptional(clazz, transformer.chain(Optional::of));
        }

        @SuppressWarnings("unchecked")
        public <C extends I> Builder<I, O, MaI, MaO, RI, RO> instanceOfOptional(Class<C> clazz, Transformer<? super C, Optional<? extends O>> transformer) {
            return transform(i -> clazz.isInstance(i) ? transformer.apply((C) i) : Optional.empty());
        }

        @SafeVarargs
        public final Builder<I, O, MaI, MaO, RI, RO> groupedCatch(BiFunction<Transformer<? super I, ? extends O>, Transformer<? super I, ? extends O>, Transformer<? super I, ? extends O>> chainer,
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

        public Builder<I, O, MaI, MaO, RI, RO> catchAll(Transformer<? super I, ? extends O> finalTransformer) {
            return transform(_ -> true, finalTransformer);
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(I input, O to) {
            return transform(Predicate.isEqual(input), to);
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(Predicate<I> inputPredicate, O to) {
            return transform(inputPredicate, _ -> to);
        }

        public Builder<I, O, MaI, MaO, RI, RO> transformBuilder(Predicate<I> inputPredicate, StrongBuilder<I, O> builder) {
            return transform(inputPredicate, strong(builder));
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(Predicate<I> inputPredicate, Transformer<? super I, ? extends O> transformer) {
            return transform(i -> inputPredicate.test(i) ? Optional.of(transformer.apply(i)) : Optional.empty());
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(I input, Transformer<? super RI, ? extends RO> transformer) {
            assert root != null : "use transform(I, O) as this is the root";
            root.root.transform(rootInput -> root.inputMapper.apply(rootInput).map(i -> i.equals(input)).orElse(false) ? Optional.of(transformer.apply(rootInput)) : Optional.empty());
            return this;
        }

        public Builder<I, O, MaI, MaO, RI, RO> transformBuilder(WeakBuilder<I, O> builder) {
            return transform(weak(builder));
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(Transformer<? super I, Optional<? extends O>> transformer) {
            if (root != null) {
                root.delegate(transformer);
            } else {
                assert transformers != null;
                transformers.add(transformer);
            }
            return this;
        }

        public Builder<I, O, MaI, MaO, RI, RO> check(Predicate<? super O> predicate) {
            return flatMapOutput(output -> predicate.test(output) ? Optional.of(output) : Optional.empty());
        }

        public <MI> Builder<MI, O, MaI, MaO, RI, RO> mapInput(Transformer<? super I, ? extends MI> inputMapper) {
            return flatMap(inputMapper.chain(Optional::of), Optional::of);
        }

        public <MI> Builder<MI, O, MaI, MaO, RI, RO> flatMapInput(Transformer<? super I, Optional<? extends MI>> inputMapper) {
            return flatMap(inputMapper, Optional::of);
        }

        public <MO> Builder<I, MO, MaI, MaO, RI, RO> mapOutput(Transformer<? super MO, ? extends O> outputMapper) {
            return flatMap(Optional::of, outputMapper.chain(Optional::of));
        }

        public <MO> Builder<I, MO, MaI, MaO, RI, RO> flatMapOutput(Transformer<? super MO, Optional<? extends O>> outputMapper) {
            return flatMap(Optional::of, outputMapper);
        }

        @SuppressWarnings("unchecked")
        public <MI, MO> Builder<MI, MO, MaI, MaO, RI, RO> flatMap(Transformer<? super I, Optional<? extends MI>> inputMapper, Transformer<? super MO, Optional<? extends O>> outputMapper) {
            Root<MI, MO, RI, RO> mappedRoot;
            if (root != null) {
                mappedRoot = root.map(inputMapper, outputMapper);
            } else {
                mappedRoot = new Root<>(root(),
                        (Transformer<? super RI, Optional<? extends MI>>) inputMapper,
                        (Transformer<? super MO, Optional<? extends RO>>) (Object) outputMapper);
            }
            return new Builder<>(mappedRoot, mark);
        }

        public Builder<I, O, I, O, RI, RO> mark() {
            return marked(this);
        }

        public Builder<MaI, MaO, MaI, MaO, RI, RO> reset() {
            assert mark != null : "can only perform reset if mark is set";
            return mark;
        }

        @SuppressWarnings("unchecked")
        public Builder<RI, RO, ?, ?, RI, RO> root() {
            if (root != null) {
                return root.root;
            }
            return (Builder<RI, RO, ?, ?, RI, RO>) this;
        }

        public Transformer<RI, RO> finish(O fallback) {
            return finish(_ -> fallback);
        }

        public Transformer<RI, RO> finish(Transformer<? super I, ? extends O> finalTransformer) {
            List<Transformer<? super RI, Optional<? extends RO>>> transformers = gatherTransformers();
            return input -> {
                for (Transformer<? super RI, Optional<? extends RO>> transformer : transformers) {
                    Optional<? extends RO> result = transformer.apply(input);
                    if (result.isPresent()) {
                        return result.get();
                    }
                }
                return mapInput(input).map(finalTransformer).flatMap(this::mapOutput)
                        .orElseThrow(() -> new NoSuchElementException("finalTransformer returned an output not supported by the root"));
            };
        }

        public Transformer<RI, Optional<? extends RO>> build() {
            List<Transformer<? super RI, Optional<? extends RO>>> transformers = gatherTransformers();
            return input -> {
                for (Transformer<? super RI, Optional<? extends RO>> transformer : transformers) {
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

        private List<Transformer<? super RI, Optional<? extends RO>>> gatherTransformers() {
            return List.copyOf(Objects.requireNonNull(root().transformers));
        }

        private record Root<I, O, RI, RO>(Builder<RI, RO, ?, ?, RI, RO> root,
                                          Transformer<? super RI, Optional<? extends I>> inputMapper,
                                          Transformer<? super O, Optional<? extends RO>> outputMapper) {

            private <MI, MO> Root<MI, MO, RI, RO> map(Transformer<? super I, Optional<? extends MI>> inputMapper, Transformer<? super MO, Optional<? extends O>> outputMapper) {
                return new Root<>(root, this.inputMapper.chain(optional -> optional.flatMap(inputMapper)),
                        outputMapper.chain(optional -> optional.flatMap(this.outputMapper)));
            }

            private void delegate(Transformer<? super I, Optional<? extends O>> transformer) {
                root.transform(inputMapper.chain(i -> i.flatMap(transformer).flatMap(outputMapper)));
            }
        }
    }
}
