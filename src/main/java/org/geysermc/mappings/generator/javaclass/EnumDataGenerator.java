package org.geysermc.mappings.generator.javaclass;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.definitions.protocol.EnumConstant;
import org.geysermc.mappings.generator.MappingsGenerator;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.resources.DownloadedSamples;
import org.geysermc.mappings.util.MappingsUtil;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EnumDataGenerator extends MappingsGenerator<String> {
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

    public static Function<PackOutput, EnumDataGenerator> factory(String name, FileType<String> fileType, Supplier<CompletableFuture<Stream<String>>> valuesSupplier) {
        return output -> new EnumDataGenerator(output, fileType, name, valuesSupplier);
    }

    public static <E> Function<PackOutput, EnumDataGenerator> factory(String name, FileType<String> fileType, Supplier<Stream<E>> valuesSupplier,
                                                                      Function<E, String> toString) {
        return factory(name, fileType, () -> CompletableFuture.completedFuture(valuesSupplier.get().map(toString)));
    }

    public static <E> Function<PackOutput, EnumDataGenerator> factory(String name, FileType<String> fileType, Supplier<Stream<E>> valuesSupplier,
                                                                      Function<E, String> toString, Function<E, Stream<Object>> argsSupplier) {
        return factory(name, fileType, valuesSupplier,
                value -> toString.apply(value) + "(" + argsSupplier.apply(value).map(Object::toString).collect(Collectors.joining(", ")) + ")");
    }

    public static <E extends StringRepresentable> Function<PackOutput, EnumDataGenerator> enumFactory(String name, FileType<String> fileType, Supplier<E[]> valuesSupplier) {
        return factory(name, fileType, () -> Arrays.stream(valuesSupplier.get()), constant -> constant.getSerializedName().toUpperCase(Locale.ROOT));
    }

    public static <E extends Enum<E>> Function<PackOutput, EnumDataGenerator> rawEnumFactory(String name, FileType<String> fileType, Supplier<E[]> valuesSupplier) {
        return factory(name, fileType, () -> Arrays.stream(valuesSupplier.get()), E::name);
    }

    public static <T> Function<PackOutput, EnumDataGenerator> registryFactory(String name, FileType<String> fileType, Registry<T> builtInRegistry) {
        return factory(name, fileType, builtInRegistry::listElementIds, EnumDataGenerator::keyToConstant);
    }

    public static <T> BiFunction<PackOutput, CompletableFuture<RegistryAccess>, EnumDataGenerator> registryFactory(String name, FileType<String> fileType,
                                                                                                                   ResourceKey<? extends Registry<T>> registry) {
        return (output, registryFuture) -> new EnumDataGenerator(output, fileType, name,
                () -> registryFuture.thenApply(registries -> registries.lookupOrThrow(registry).listElementIds().map(EnumDataGenerator::keyToConstant)));
    }

    public static BiFunction<PackOutput, CompletableFuture<BedrockSamples>, EnumDataGenerator> samplesFactory(String name, FileType<String> fileType,
                                                                                                              DownloadedSamples.FileSystemUser<Stream<String>> extractor) {
        return (output, samplesFuture) -> new EnumDataGenerator(output, fileType, name,
                () -> samplesFuture.thenCompose(samples -> samples.samples().open(extractor)));
    }

    public static BiFunction<PackOutput, CompletableFuture<BedrockSamples>, EnumDataGenerator> protocolFactory(String name, FileType<String> fileType,
                                                                                                               FileType<Set<EnumConstant>> source, Predicate<EnumConstant> predicate) {
        return (output, samplesFuture) -> new EnumDataGenerator(output, fileType, name,
                () -> samplesFuture.thenCompose(samples
                        -> samples.protocol().open(protocolData
                        -> protocolData.readFileOrThrow(source).thenApply(constants
                        -> constants.stream().filter(predicate).map(EnumConstant::name).map(MappingsUtil::camelToUpperCaseSnakeCase)))));
    }

    private static String keyToConstant(ResourceKey<?> key) {
        return key.identifier().getPath().toUpperCase(Locale.ROOT);
    }
}
