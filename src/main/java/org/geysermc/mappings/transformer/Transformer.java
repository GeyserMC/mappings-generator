package org.geysermc.mappings.transformer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
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

    static <T> Transformer<T, T> identity() {
        return t -> t;
    }

    static <I, O> Transformer<I, Optional<? extends O>> weak(Consumer<Builder<I, O, ?, ?, I, O>> builder) {
        Builder<I, O, ?, ?, I, O> baseBuilder = new Builder<>();
        builder.accept(baseBuilder);
        return baseBuilder.build();
    }

    static <I, O> Transformer<I, O> strong(Function<Builder<I, O, ?, ?, I, O>, Transformer<I, O>> builder) {
        return builder.apply(new Builder<>());
    }

    @FunctionalInterface
    interface Creator<BI, I, O> {

        Transformer<I, O> create(BI builderInput);
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
            return instanceOf(clazz, _ -> Optional.of(output));
        }

        public <C extends I> Builder<I, O, MaI, MaO, RI, RO> instanceOfBuilder(Class<C> clazz, Consumer<Builder<C, O, ?, ?, C, O>> builder) {
            return instanceOf(clazz, weak(builder));
        }

        @SuppressWarnings("unchecked")
        public <C extends I> Builder<I, O, MaI, MaO, RI, RO> instanceOf(Class<C> clazz, Transformer<C, Optional<? extends O>> transformer) {
            return transform(i -> clazz.isInstance(i) ? transformer.apply((C) i) : Optional.empty());
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(I input, O to) {
            return transform(Predicate.isEqual(input), _ -> to);
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(Predicate<I> inputPredicate, Transformer<? super I, ? extends O> transformer) {
            return transform(i -> inputPredicate.test(i) ? Optional.of(transformer.apply(i)) : Optional.empty());
        }

        public Builder<I, O, MaI, MaO, RI, RO> transform(I input, Transformer<? super RI, ? extends RO> transformer) {
            assert root != null : "use transform(I, O) as this is the root";
            root.root.transform(rootInput -> root.inputMapper.apply(rootInput).equals(input) ? Optional.of(transformer.apply(rootInput)) : Optional.empty());
            return this;
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
            return mapBoth(inputMapper, Optional::of);
        }

        public <MO> Builder<I, MO, MaI, MaO, RI, RO> mapOutput(Transformer<? super MO, ? extends O> outputMapper) {
            return mapBoth(Transformer.identity(), outputMapper.chain(Optional::of));
        }

        public <MO> Builder<I, MO, MaI, MaO, RI, RO> flatMapOutput(Transformer<? super MO, Optional<? extends O>> outputMapper) {
            return mapBoth(Transformer.identity(), outputMapper);
        }

        @SuppressWarnings("unchecked")
        public <MI, MO> Builder<MI, MO, MaI, MaO, RI, RO> mapBoth(Transformer<? super I, ? extends MI> inputMapper, Transformer<? super MO, Optional<? extends O>> outputMapper) {
            Root<MI, MO, RI, RO> mappedRoot;
            if (root != null) {
                mappedRoot = root.map(inputMapper, outputMapper);
            } else {
                mappedRoot = new Root<>(root(),
                        (Transformer<? super RI, ? extends MI>) inputMapper,
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

        public void lastly(Transformer<? super I, ? extends O> finalTransformer) {
            transform(_ -> true, finalTransformer);
        }

        public Transformer<RI, RO> finish(Transformer<? super RI, ? extends RO> finalTransformer) {
            List<Transformer<? super RI, Optional<? extends RO>>> transformers = gatherTransformers();
            return input -> {
                for (Transformer<? super RI, Optional<? extends RO>> transformer : transformers) {
                    Optional<? extends RO> result = transformer.apply(input);
                    if (result.isPresent()) {
                        return result.get();
                    }
                }
                return finalTransformer.apply(input);
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

        private List<Transformer<? super RI, Optional<? extends RO>>> gatherTransformers() {
            return List.copyOf(Objects.requireNonNull(root().transformers));
        }

        private record Root<I, O, RI, RO>(Builder<RI, RO, ?, ?, RI, RO> root,
                                          Transformer<? super RI, ? extends I> inputMapper,
                                          Transformer<? super O, Optional<? extends RO>> outputMapper) {

            private <MI, MO> Root<MI, MO, RI, RO> map(Transformer<? super I, ? extends MI> inputMapper, Transformer<? super MO, Optional<? extends O>> outputMapper) {
                return new Root<>(root, inputMapper.compose(this.inputMapper), outputMapper.chain(optional -> optional.flatMap(this.outputMapper)));
            }

            private void delegate(Transformer<? super I, Optional<? extends O>> transformer) {
                root.transform(transformer.compose(inputMapper).chain(optional -> optional.flatMap(outputMapper)));
            }
        }
    }
}
