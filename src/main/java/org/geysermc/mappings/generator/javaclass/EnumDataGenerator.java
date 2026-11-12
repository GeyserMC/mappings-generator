package org.geysermc.mappings.generator.javaclass;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import org.geysermc.mappings.EnumDataGenerators;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.ProviderGroupBuilder;
import org.geysermc.mappings.definitions.protocol.EnumConstant;
import org.geysermc.mappings.generator.MappingsGenerator;
import org.geysermc.mappings.names.TypeRenamer;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.util.MappingsUtil;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// A generator generating a stub enum to a `.java` file. Use the static factory methods to create instances,
/// register them in {@link EnumDataGenerators}.
public final class EnumDataGenerator extends MappingsGenerator<String> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String name;
    private final Supplier<CompletableFuture<Stream<String>>> valuesSupplier;

    private EnumDataGenerator(PackOutput output, FileType<String> fileType, String name, Supplier<CompletableFuture<Stream<String>>> valuesSupplier) {
        super(output, fileType);
        this.name = name;
        this.valuesSupplier = valuesSupplier;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return valuesSupplier.get().thenCompose(values -> saveFile(cache, values.collect(Collectors.joining(",\n")) + ";\n"));
    }

    @Override
    public String getName() {
        return name + " Generator";
    }

    /// Returns a factory for an {@link EnumDataGenerator} which generates an enum from the `valuesSupplier`, using the `toString` function to turn the values into string enum constants.
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param valuesSupplier a supplier supplying a stream of values to write
    /// @param toString a function turning an {@link E} into an enum constant (as a {@link String})
    /// @param <E> the type of the enum
    /// @return the factory
    public static <E> Function<PackOutput, EnumDataGenerator> factory(String name, FileType<String> fileType, Supplier<Stream<E>> valuesSupplier,
                                                                      Function<E, String> toString) {
        return output -> new EnumDataGenerator(output, fileType, name, () -> CompletableFuture.completedFuture(valuesSupplier.get().map(toString)));
    }

    /// Returns a factory for an {@link EnumDataGenerator} which generates an enum from the `valuesSupplier`, using the `toString` function to turn the values into string enum constants,
    /// and the `argsSupplier` function to obtain arguments for each enum constant.
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param valuesSupplier a supplier supplying a stream of values to write
    /// @param toString a function turning an {@link E} into an enum constant (as a {@link String})
    /// @param argsSupplier a function returning a stream of arguments (as {@link Object}s) for an {@link E}
    /// @param <E> the type of the enum
    /// @return the factory
    public static <E> Function<PackOutput, EnumDataGenerator> factory(String name, FileType<String> fileType, Supplier<Stream<E>> valuesSupplier,
                                                                      Function<E, String> toString, Function<E, Stream<Object>> argsSupplier) {
        return factory(name, fileType, valuesSupplier,
                value -> toString.apply(value) + "(" + argsSupplier.apply(value).map(Object::toString).collect(Collectors.joining(", ")) + ")");
    }

    /// Returns a factory for an {@link EnumDataGenerator} which uses an enum implementing {@link StringRepresentable} as source.
    ///
    /// {@link StringRepresentable} is commonly used on enums within MC's source code to serialise enums to a string. {@link StringRepresentable#getSerializedName()}
    /// is used to get the name of the enum constants (the returned value is turned upper case first).
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param valuesSupplier a supplier supplying an array of values to write (usually `Enum#values()`)
    /// @param <E> the type of the enum
    /// @return the factory
    public static <E extends StringRepresentable> Function<PackOutput, EnumDataGenerator> enumFactory(String name, FileType<String> fileType, Supplier<E[]> valuesSupplier) {
        return factory(name, fileType, () -> Arrays.stream(valuesSupplier.get()), constant -> constant.getSerializedName().toUpperCase(Locale.ROOT));
    }

    /// Returns a factory for an {@link EnumDataGenerator} which uses an enum as source.
    ///
    /// {@link Enum#name()} is used to get the name of the enum constants.
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param valuesSupplier a supplier supplying an array of values to write (usually `Enum#values()`)
    /// @param <E> the type of the enum
    /// @return the factory
    public static <E extends Enum<E>> Function<PackOutput, EnumDataGenerator> rawEnumFactory(String name, FileType<String> fileType, Supplier<E[]> valuesSupplier) {
        return factory(name, fileType, () -> Arrays.stream(valuesSupplier.get()), E::name);
    }

    /// Returns a factory for an {@link EnumDataGenerator} which uses a vanilla built-in registry as source.
    ///
    /// {@link Identifier#getPath()} is used to get the name of the enum constants (the returned value is turned upper case first).
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param builtInRegistry the registry to write
    /// @param <T> the type of the registry
    /// @return the factory
    public static <T> Function<PackOutput, EnumDataGenerator> registryFactory(String name, FileType<String> fileType, Registry<T> builtInRegistry) {
        return factory(name, fileType, builtInRegistry::listElementIds, EnumDataGenerator::keyToConstant);
    }

    /// Returns a registry-dependent factory for an {@link EnumDataGenerator} which uses a vanilla registry as source.
    ///
    /// {@link Identifier#getPath()} is used to get the name of the enum constants (the returned value is turned upper case first).
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param registry the registry to write
    /// @param <T> the type of the registry
    /// @return the factory
    public static <T> BiFunction<PackOutput, CompletableFuture<RegistryAccess>, EnumDataGenerator> registryFactory(String name, FileType<String> fileType,
                                                                                                                   ResourceKey<? extends Registry<T>> registry) {
        return (output, registryFuture) -> new EnumDataGenerator(output, fileType, name,
                () -> registryFuture.thenApply(registries -> registries.lookupOrThrow(registry).listElementIds().map(EnumDataGenerator::keyToConstant)));
    }

    /// Returns a factory for an {@link EnumDataGenerator} which uses a vanilla registry as source, and a Bedrock enum as order.
    ///
    /// The registry elements are first renamed using the given `renamer`, and are then looked up in the given Bedrock enum to obtain the
    /// element's ordinal. {@link Identifier#getPath()} is used to get the name of the enum constants (vanilla name, the returned value is turned upper case first).
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param registryKey the registry to write
    /// @param bedrockSource the file in `bedrock-protocol-docs` containing the Bedrock enum
    /// @param renamer a {@link TypeRenamer} renaming the Java {@link Identifier} to an entry in the Bedrock enum
    /// @param <T> the type of the registry
    /// @return the factory
    public static <T> ProviderGroupBuilder.ProviderFactory<EnumDataGenerator> protocolBasedRegistry(String name, FileType<String> fileType,
                                                                                                    ResourceKey<? extends Registry<T>> registryKey,
                                                                                                    FileType<Set<EnumConstant>> bedrockSource,
                                                                                                    TypeRenamer<Identifier, String> renamer) {
        return (output, registryFuture, samplesFuture) -> new EnumDataGenerator(output, fileType, name,
                () -> CompletableFuture.allOf(registryFuture, samplesFuture).thenCompose(_ -> {
                    Registry<T> registry = registryFuture.join().lookupOrThrow(registryKey);
                    BedrockSamples samples = samplesFuture.join();
                    return samples.protocol().open(system -> system.readFileOrThrow(bedrockSource))
                            .thenApply(bedrockEnum -> registry.listElementIds()
                                    .map(key -> {
                                        try {
                                            return Optional.of(Pair.of(keyToConstant(key), EnumConstant.findId(bedrockEnum, renamer.get(key.identifier()))));
                                        } catch (IllegalArgumentException exception) {
                                            LOGGER.error("Missed mapping for {} of registry {} in {}!", key.identifier(), registryKey.identifier(), name);
                                            return Optional.<Pair<String, Integer>>empty();
                                        }
                                    })
                                    .flatMap(Optional::stream)
                                    .sorted(Comparator.comparingInt(Pair::getSecond))
                                    .map(Pair::getFirst));
                }));
    }

    /// Returns a factory for an {@link EnumDataGenerator} which uses a Bedrock enum as source.
    ///
    /// The enum constants from the source are first renamed to upper case-snake case before writing.
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param source the source file in `bedrock-protocol-docs`
    /// @param predicate a predicate returning `true` if an {@link EnumConstant} should be included
    /// @return the factory
    public static BiFunction<PackOutput, CompletableFuture<BedrockSamples>, EnumDataGenerator> protocolFactory(String name, FileType<String> fileType,
                                                                                                               FileType<Set<EnumConstant>> source, Predicate<EnumConstant> predicate) {
        return (output, samplesFuture) -> new EnumDataGenerator(output, fileType, name,
                () -> samplesFuture.thenCompose(samples
                        -> samples.protocol().open(protocolData
                        -> protocolData.readFileOrThrow(source).thenApply(constants
                        -> constants.stream().filter(predicate).map(EnumConstant::name).map(MappingsUtil::camelToUpperCaseSnakeCase)))));
    }

    /// Returns a factory for an {@link EnumDataGenerator} which uses a file in `bedrock-samples` as source.
    ///
    /// @param name the name of the generator
    /// @param fileType the file the generator writes to
    /// @param source the source file in `bedrock-samples`
    /// @param extractor a function that turns the read source file into a list of strings to write
    /// @param <T> the type of the source file
    /// @return the factory
    public static <T> BiFunction<PackOutput, CompletableFuture<BedrockSamples>, EnumDataGenerator> samplesFactory(String name, FileType<String> fileType,
                                                                                                                  FileType<T> source, Function<T, Stream<String>> extractor) {
        return (output, samplesFuture) -> new EnumDataGenerator(output, fileType, name,
                () -> samplesFuture.thenCompose(samples -> samples.samples().open(system -> system.readFileOrThrow(source)))
                        .thenApply(extractor));
    }

    private static String keyToConstant(ResourceKey<?> key) {
        return key.identifier().getPath().toUpperCase(Locale.ROOT);
    }
}
