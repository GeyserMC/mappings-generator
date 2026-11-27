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
    /// @see Factory
    static <I, O> Transformer<I, O> of(Builder<I, O> builder) {
        return builder.create(new Factory<>());
    }

    /// Creates a {@link Transformer.Maybe} using a {@link Factory}
    ///
    /// @param builder the function building the transformer using a {@link Factory}
    /// @param <I> the transformer's input
    /// @param <O> the transformer's output
    /// @return the built transformer
    /// @see Factory
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
    /// @see Factory
    @FunctionalInterface
    interface Builder<I, O> extends Creator<Factory<I, O, ?, ?, I, O>, I, O> {}

    /// A {@link Transformer.Maybe} is a transformer returning an {@link Optional}. This interface provides
    /// extra methods for chaining "maybe transformers" with other maybe transformers.
    interface Maybe<I, O> extends Transformer<I, Optional<? extends O>> {

        /// {@inheritDoc}
        @Override
        default <MI> Maybe<MI, O> compose(Transformer<? super MI, ? extends I> before) {
            return input -> apply(before.apply(input));
        }

        /// {@inheritDoc}
        @Override
        default <MI> Maybe<MI, O> composeBuilder(Transformer.Builder<? super MI, ? extends I> before) {
            return compose(of(before));
        }

        /// Combines this transformer and the given transformer by applying the given transformer first, then this one.
        ///
        /// @param before the transformer to apply before this one
        /// @param <MI> the new ("mapped") input
        /// @return the combined transformer
        default <MI> Maybe<MI, O> maybeCompose(Maybe<? super MI, ? extends I> before) {
            return input -> before.apply(input).flatMap(this);
        }

        /// Shorthand for {@link Maybe#maybeCompose(Maybe)}, taking a {@link Maybe.Builder}.
        ///
        /// @param before the transformer to apply before this one
        /// @param <MI> the new ("mapped") input
        /// @return the combined transformer
        /// @see Maybe#maybeCompose(Maybe)
        default <MI> Maybe<MI, O> maybeComposeBuilder(Builder<? super MI, ? extends I> before) {
            return maybeCompose(Transformer.maybe(before));
        }

        /// Combines this transformer and the given transformer by applying the given transformer after this one.
        ///
        /// Use this method instead of {@link Transformer#chain(Transformer)} to chain this transformer with a normal {@link Transformer}.
        ///
        /// @param after the transformer to apply after this one
        /// @param <MO> the new ("mapped") output
        /// @return the combined transformer
        default <MO> Maybe<I, MO> maybeMap(Transformer<? super O, ? extends MO> after) {
            return input -> apply(input).map(after);
        }

        /// Shorthand for {@link Maybe#maybeMap(Transformer)}, taking a {@link Transformer.Builder}.
        ///
        /// @param after the transformer to apply after this one
        /// @param <MO> the new ("mapped") output
        /// @return the combined transformer
        /// @see Maybe#maybeMap(Transformer)
        default <MO> Maybe<I, MO> maybeMapBuilder(Transformer.Builder<? super O, ? extends MO> after) {
            return maybeMap(of(after));
        }

        /// Combines this transformer and the given transformer by applying the given transformer after this one.
        ///
        /// @param after the transformer to apply after this one
        /// @param <MO> the new ("mapped") output
        /// @return the combined transformer
        default <MO> Maybe<I, MO> maybeChain(Maybe<? super O, ? extends MO> after) {
            return input -> apply(input).flatMap(after);
        }

        /// Shorthand for {@link Maybe#maybeChain(Maybe)}, taking a {@link Maybe.Builder}.
        ///
        /// @param after the transformer to apply after this one
        /// @param <MO> the new ("mapped") output
        /// @return the combined transformer
        /// @see Maybe#maybeChain(Maybe)
        default <MO> Maybe<I, MO> maybeChainBuilder(Builder<? super O, ? extends MO> after) {
            return maybeChain(Transformer.maybe(after));
        }

        /// Turns this transformer into a normal {@link Transformer} by returning the given fallback value when this
        /// transformer returns an empty {@link Optional}.
        ///
        /// @param fallback the fallback value to use
        /// @return the normal {@link Transformer}
        default Transformer<I, O> orElse(O fallback) {
            return orElse(_ -> fallback);
        }

        /// Turns this transformer into a normal {@link Transformer} by using the given fallback transformer when this
        /// transformer returns an empty {@link Optional}.
        ///
        /// @param fallback the fallback transformer to use
        /// @return the normal {@link Transformer}
        default Transformer<I, O> orElse(Transformer<? super I, ? extends O> fallback) {
            return input -> {
                Optional<? extends O> result = apply(input);
                if (result.isPresent()) {
                    return result.get();
                }
                return fallback.apply(input);
            };
        }

        /// Turns this transformer into a normal {@link Transformer} by throwing a {@link NoSuchElementException}
        /// when this transformer returns an empty {@link Optional}.
        ///
        /// @return the normal {@link Transformer}
        default Transformer<I, O> orElseThrow() {
            return orElse(_ -> {
                throw new NoSuchElementException("Maybe transformer returned no element, and orElseThrow() was used");
            });
        }

        /// @param <T> the type of the transformer
        /// @return a transformer that returns its input, wrapped in an {@link Optional}
        static <T> Maybe<T, T> identity() {
            return Optional::of;
        }

        /// @param <T> the type of the transformer
        /// @return a transformer that always returns an empty {@link Optional}
        static <T> Maybe<T, T> empty() {
            return _ -> Optional.empty();
        }

        /// A {@link Transformer.Builder} is a function building a maybe transformer using a {@link Factory}.
        ///
        /// @param <I> the transformer's input
        /// @param <O> the transformer's output
        /// @see Factory
        @FunctionalInterface
        interface Builder<I, O> {

            void build(Factory<I, O, ?, ?, I, O> factory);
        }
    }

    /// A {@link Factory} can be used to construct a {@link Transformer} or a {@link Maybe} by combining multiple {@link Transformer}s
    /// together. Factories can be used in multiple ways:
    ///
    /// - You can register simple input->output mappings, through {@link Factory#transform(Object, Object)} or {@link Factory#transform(Predicate, Object)}.
    /// - You can register transformers that transform only certain inputs, for example through {@link Factory#transform(Predicate, Transformer)}.
    /// - You can register transformers that transform a subclass of the input, for example through {@link Factory#instanceOf(Class, Transformer)}.
    /// - You can register a list of transformers, all of which will be tried in succession, through {@link Factory#tryAllPermutations(BiFunction, Maybe[])}.
    ///
    /// This is a non-exhaustive list, the factory API is extensive and should allow for most use cases. Each method is thoroughly documented
    /// with use cases, so you should have little difficulty finding the right one for you.
    ///
    /// Factories are order-dependent: when constructing a {@link Transformer} or a {@link Maybe} using a factory,
    /// the constructed transformer will loop over each registered transformer, in the order they were registered in.
    ///
    /// A factory allows registering input- and output transformers. Registering such a transformer creates a new "child" factory instance,
    /// which delegates back to the "root" factory to register its transformers (which are composed/chained with the input/output mappers).
    /// "Child" factories can be marked using {@link Factory#mark()}, so they can be reset to later using {@link Factory#reset()}.
    /// The uses of these are a little complicated to explain, but you can refer to a transformer like in {@link org.geysermc.mappings.transformer.instance.ParticleMappings}
    /// for an example.
    ///
    /// Factories cannot be created directly: they must be used through {@link Transformer#of(Builder)} or {@link Transformer#maybe(Maybe.Builder)}.
    ///
    /// @see Transformer#of(Builder)
    /// @see Transformer#maybe(Maybe.Builder)
    /// @param <I> the input of the current factory, it can be the same as the root's input, or a transformed one
    /// @param <O> the output of the current factory, it can be the same as the root's output, or a transformed one
    /// @param <MaI> the input of the currently marked factory, it can be the same as this one. It will be a wildcard initially
    /// @param <MaO> the output of the currently marked factory, it can be the same as this one. It will be a wildcard initially
    /// @param <RI> the input of the root factory, which will be the same as the current input if this factory is the root
    /// @param <RO> the output of the root factory, which will be the same as the current input if this factory is the root
    // Prepare to witness the beast I have created
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

        /// Registers a transformer returning the given output for each input that is an instance of {@link C}.
        ///
        /// @param clazz the class of {@link C}
        /// @param output the output to return for each input that is an instance of {@link C}
        /// @param <C> the type to check for
        /// @return this factory
        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> instanceOf(Class<C> clazz, O output) {
            return instanceOf(clazz, _ -> output);
        }

        /// Shorthand for {@link Factory#instanceOf(Class, Transformer)}.
        ///
        /// @param clazz the class of {@link C}
        /// @param builder the transformer to use for each input that is an instance of {@link C}
        /// @param <C> the type to check for
        /// @return this factory
        /// @see Factory#instanceOf(Class, Transformer)
        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> instanceOfBuilder(Class<C> clazz, Builder<C, O> builder) {
            return instanceOf(clazz, of(builder));
        }

        /// Registers a transformer used for each input that is an instance of {@link C}.
        ///
        /// @param clazz the class of {@link C}
        /// @param transformer the transformer to use for each input that is an instance of {@link C}
        /// @param <C> the type to check for
        /// @return this factory
        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> instanceOf(Class<C> clazz, Transformer<? super C, ? extends O> transformer) {
            return maybeInstanceOf(clazz, transformer.maybe());
        }

        /// Shorthand for {@link Factory#maybeInstanceOf(Class, Maybe)}.
        ///
        /// @param clazz the class of {@link C}
        /// @param builder the transformer to try for each input that is an instance of {@link C}
        /// @param <C> the type to check for
        /// @return this factory
        /// @see Factory#maybeInstanceOf(Class, Maybe)
        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> maybeInstanceOfBuilder(Class<C> clazz, Maybe.Builder<C, O> builder) {
            return maybeInstanceOf(clazz, maybe(builder));
        }

        /// Registers a maybe transformer tried for each input that is an instance of {@link C}.
        ///
        /// When the transformer returns an empty {@link Optional} for an input, it is skipped.
        ///
        /// @param clazz the class of {@link C}
        /// @param transformer the transformer to try for each input that is an instance of {@link C}
        /// @param <C> the type to check for
        /// @return this factory
        @SuppressWarnings("unchecked")
        public <C extends I> Factory<I, O, MaI, MaO, RI, RO> maybeInstanceOf(Class<C> clazz, Maybe<? super C, ? extends O> transformer) {
            return maybeTryAll(i -> clazz.isInstance(i) ? transformer.apply((C) i) : Optional.empty());
        }

        /// Shorthand for {@link Factory#tryAllPermutations(BiFunction, int, Maybe[])}, using `2` as `maxDepth`.
        ///
        /// @param chainer a method chaining 2 {@link Maybe}s together. Should usually be a reference to {@link Maybe#maybeChain(Maybe)}
        /// @param transformers the array of transformers to generate permutations for
        /// @return this factory
        /// @see Factory#tryAllPermutations(BiFunction, int, Maybe[])
        @SafeVarargs
        public final Factory<I, O, MaI, MaO, RI, RO> tryAllPermutations(BiFunction<Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>, Maybe<? super I, ? extends O>> chainer,
                                                                        Maybe<? super I, ? extends O>... transformers) {
            return tryAllPermutations(chainer, 2, transformers);
        }

        /// Registers permutations of the given array of transformers, chained using the `chainer`, until the given `maxDepth` is reached.
        ///
        /// This method is useful to try various transformers on their own or combined with each other, in various orders.
        /// The `maxDepth` variable controls the depth of the permutations: registered transformers will not be longer than this number. It should be preferred
        /// to keep this number low to reduce performance costs.
        ///
        /// Because the transformers are attempted on all inputs, they could block following transformers
        /// from ever being used. As such, at least one transformer must return an empty {@link Optional}
        /// for at least one input, or this factory must have an input/output mapper returning an
        /// empty {@link Optional} for at least one input/output (it must be a "checked" factory). See {@link Factory#maybeTryAll(Maybe)}
        /// for more details.
        ///
        /// When a constructed transformer returns an empty {@link Optional} for an input, it is skipped.
        ///
        /// @param chainer a method chaining 2 {@link Maybe}s together. Should usually be a reference to {@link Maybe#maybeChain(Maybe)}
        /// @param maxDepth the maximum depth of permutations generated
        /// @param transformers the array of transformers to generate permutations for
        /// @return this factory
        /// @see Factory#maybeTryAll(Maybe)
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

        /// Registers a transformer applied to the root input, used for the given input.
        ///
        /// This method has limited use cases, but it was used at least once in {@link org.geysermc.mappings.transformer.instance.BlockNames}.
        ///
        /// This method will throw when this factory is the root: use {@link Factory#transform(Object, Object)} instead,
        /// as the input is known when using this method, and the output must always be the same given an input.
        ///
        /// @param input the input to check for
        /// @param transformer the transformer applied to the root input
        /// @return this factory
        public Factory<I, O, MaI, MaO, RI, RO> transform(I input, Transformer<? super RI, ? extends RO> transformer) {
            assert root != this : "use transform(I, O) as this is the root";
            root.maybeTryAll(rootInput -> mappers.inputMapper.apply(rootInput).map(i -> i.equals(input)).orElse(false) ? Optional.of(transformer.apply(rootInput)) : Optional.empty());
            return this;
        }

        /// Registers a transformer using {@link Factory#transform(Predicate, Object)}, matching for the given input and returning
        /// the given output.
        ///
        /// This can be used to essentially register a "hardcoded" mapping for a given input.
        ///
        /// @param input the input to match for
        /// @param to the output to return
        /// @return this factory
        /// @see Factory#transform(Predicate, Object)
        public Factory<I, O, MaI, MaO, RI, RO> transform(I input, O to) {
            return transform(Predicate.isEqual(input), to);
        }

        /// Registers a transformer returning the given output for each input matching the given predicate.
        ///
        /// @param inputPredicate the predicate to match for
        /// @param to the output to return for each input matching the input predicate
        /// @return this factory
        public Factory<I, O, MaI, MaO, RI, RO> transform(Predicate<I> inputPredicate, O to) {
            if (root != this) {
                // Ensure the output is valid
                mappers.outputMapper.apply(to).orElseThrow(() -> new IllegalStateException("Hardcoded transformation failed to map to root: " + to));
            }
            return transform(inputPredicate, _ -> to);
        }

        /// Shorthand for {@link Factory#transform(Predicate, Transformer)}.
        ///
        /// @param inputPredicate the predicate to match for
        /// @param builder the transformer to use for each input matching the input predicate
        /// @return this factory
        /// @see Factory#transform(Predicate, Transformer)
        public Factory<I, O, MaI, MaO, RI, RO> transformBuilder(Predicate<I> inputPredicate, Builder<I, O> builder) {
            return transform(inputPredicate, of(builder));
        }

        /// Registers a transformer used for each input matching the given predicate.
        ///
        /// @param inputPredicate the predicate to match for
        /// @param transformer the transformer to use for each input matching the input predicate
        /// @return this factory
        public Factory<I, O, MaI, MaO, RI, RO> transform(Predicate<I> inputPredicate, Transformer<? super I, ? extends O> transformer) {
            return maybeTryAll(i -> inputPredicate.test(i) ? Optional.of(transformer.apply(i)) : Optional.empty());
        }

        /// Shorthand for {@link Factory#maybeTransform(Predicate, Maybe)}.
        ///
        /// @param inputPredicate the predicate to match for
        /// @param builder the transformer to try for each input matching the input predicate
        /// @return this factory
        /// @see Factory#maybeTransform(Predicate, Maybe)
        public Factory<I, O, MaI, MaO, RI, RO> maybeTransformBuilder(Predicate<I> inputPredicate, Maybe.Builder<? super I, ? extends O> builder) {
            return maybeTransform(inputPredicate, maybe(builder));
        }

        /// Registers a transformer tried for each input matching the given predicate.
        ///
        /// When the transformer returns an empty {@link Optional} for an input, it is skipped.
        ///
        /// @param inputPredicate the predicate to match for
        /// @param transformer the transformer to try for each input matching the input predicate
        /// @return this factory
        public Factory<I, O, MaI, MaO, RI, RO> maybeTransform(Predicate<I> inputPredicate, Maybe<? super I, ? extends O> transformer) {
            return maybeTryAll(transformer.maybeCompose(i -> Optional.of(i).filter(inputPredicate)));
        }

        /// Registers the given transformer tried for all inputs it will receive.
        ///
        /// Because the transformer is attempted on all inputs, it could block following transformers
        /// from ever being used. As such, this factory must have an input/output mapper returning an
        /// empty {@link Optional} for at least one input/output: it must be a "checked" factory.
        ///
        /// Put differently, this method, and other methods using it,
        /// should only really be called after a call to {@link Factory#checkOutput(Predicate)} has been made.
        /// The method can't 100% ensure this, but it will throw when this factory is the root.
        ///
        /// @param transformer the transformer to try for all inputs
        /// @return this factory
        /// @see Factory#maybeTryAll(Maybe)
        public Factory<I, O, MaI, MaO, RI, RO> tryAll(Transformer<? super I, ? extends O> transformer) {
            assert root != this : "use finish(Transformer) as this is the root";
            return maybeTryAll(transformer.maybe());
        }

        /// Registers a transformer tried for all inputs it will receive.
        ///
        /// Because the transformer is attempted on all inputs, it could block following transformers
        /// from ever being used. As such, the transformer must return an empty {@link Optional}
        /// for at least one input, or this factory must have an input/output mapper returning an
        /// empty {@link Optional} for at least one input/output (it must be a "checked" factory).
        ///
        /// When the transformer returns an empty {@link Optional} for an input, it is skipped.
        ///
        /// @param transformer the transformer to try for all inputs
        /// @return this factory
        public Factory<I, O, MaI, MaO, RI, RO> maybeTryAll(Maybe<? super I, ? extends O> transformer) {
            assert root.transformers != null;
            root.transformers.add(new TransformerWithMappers<>(mappers, transformer));
            return this;
        }

        /// Returns a child factory checking each input against the given predicate.
        ///
        /// Registered transformers to this factory and all child factories will only be used if this predicate returns true for their input.
        ///
        /// @param predicate the predicate checking the input
        /// @return the child factory
        public Factory<I, O, MaI, MaO, RI, RO> checkInput(Predicate<? super I> predicate) {
            return flatMapInput(input -> predicate.test(input) ? Optional.of(input) : Optional.empty());
        }

        /// Returns a child factory checking each output against the given predicate.
        ///
        /// Registered transformers to this factory and all child factories will only be used if this predicate returns true for their output.
        ///
        /// @param predicate the predicate checking the output
        /// @return the child factory
        public Factory<I, O, MaI, MaO, RI, RO> checkOutput(Predicate<? super O> predicate) {
            return flatMapOutput(output -> predicate.test(output) ? Optional.of(output) : Optional.empty());
        }

        /// Returns a child factory using the given transformer to map the input.
        ///
        /// Registered transformers to this factory and all child factories will be chained after the given transformer.
        ///
        /// @param inputMapper the transformer mapping the input
        /// @param <MI> the mapped input
        /// @return the child factory
        public <MI> Factory<MI, O, MaI, MaO, RI, RO> mapInput(Transformer<? super I, ? extends MI> inputMapper) {
            return flatMap(inputMapper.maybe(), Optional::of);
        }

        /// Returns a child factory using the given maybe transformer to map the input.
        ///
        /// Registered transformers to this factory and all child factories will be chained after the given maybe transformer.
        /// They will not be used if the transformer returns an empty {@link Optional} for an input.
        ///
        /// @param inputMapper the transformer mapping the input
        /// @param <MI> the mapped input
        /// @return the child factory
        public <MI> Factory<MI, O, MaI, MaO, RI, RO> flatMapInput(Maybe<? super I, ? extends MI> inputMapper) {
            return flatMap(inputMapper, Optional::of);
        }

        /// Returns a child factory using the given transformer to map the output.
        ///
        /// Registered transformers to this factory and all child factories will be chained before the given transformer.
        ///
        /// @param outputMapper the transformer mapping the output
        /// @param <MO> the mapped output
        /// @return the child factory
        public <MO> Factory<I, MO, MaI, MaO, RI, RO> mapOutput(Transformer<? super MO, ? extends O> outputMapper) {
            return flatMap(Optional::of, outputMapper.maybe());
        }

        /// Returns a child factory using the given maybe transformer to map the output.
        ///
        /// Registered transformers to this factory and all child factories will be chained before the given maybe transformer.
        /// They will not be used if the transformer returns an empty {@link Optional} for an output.
        ///
        /// @param outputMapper the transformer mapping the output
        /// @param <MO> the mapped output
        /// @return the child factory
        public <MO> Factory<I, MO, MaI, MaO, RI, RO> flatMapOutput(Maybe<? super MO, ? extends O> outputMapper) {
            return flatMap(Optional::of, outputMapper);
        }

        /// Returns a child factory using the given maybe transformers to map the input and output.
        ///
        /// Registered transformers to this factory and all child factories will be chained between the given maybe transformers.
        /// They will not be used if either transformer returns an empty {@link Optional} for an input/output.
        ///
        /// @param inputMapper the transformer mapping the input
        /// @param outputMapper the transformer mapping the output
        /// @param <MI> the mapped input
        /// @param <MO> the mapped output
        /// @return the child factory
        public <MI, MO> Factory<MI, MO, MaI, MaO, RI, RO> flatMap(Maybe<? super I, ? extends MI> inputMapper, Maybe<? super MO, ? extends O> outputMapper) {
            return new Factory<>(mappers.map(inputMapper, outputMapper), root, mark);
        }

        /// Marks this factory, to return to later by calling {@link Factory#reset()}.
        ///
        /// @return this factory, marked
        public Factory<I, O, I, O, RI, RO> mark() {
            return marked(this);
        }

        /// Returns to the previously marked factory. Throws if no factory was marked.
        ///
        /// @return the factory last marked using {@link Factory#mark()}
        public Factory<MaI, MaO, MaI, MaO, RI, RO> reset() {
            assert mark != null : "can only perform reset if mark is set";
            return mark;
        }

        /// @return the root factory
        public Factory<RI, RO, ?, ?, RI, RO> root() {
            return root;
        }

        /// Builds a {@link Transformer} using the transformers registered to this factory, returning
        /// the given fallback when as output when no registered transformer succeeded.
        ///
        /// @param fallback the fallback to return as output when no registered transformer succeeded
        /// @return the {@link Transformer}
        public Transformer<RI, RO> finish(RO fallback) {
            return finish(_ -> fallback);
        }

        /// Builds a {@link Transformer} using the transformers registered to this factory, using
        /// the given transformer when as fallback when no registered transformer succeeded.
        ///
        /// @param finalTransformer the fallback transformer to use as output when no registered transformer succeeded
        /// @return the {@link Transformer}
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

        /// Builds a {@link Maybe} using the transformers registered to this factory.
        ///
        /// @return the {@link Maybe}
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
