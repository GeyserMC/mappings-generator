package org.geysermc.mappings.generator.javaclass;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.text.WordUtils;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.generator.MappingsGenerator;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
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

    private static String keyToName(ResourceKey<?> key) {
        return WordUtils.capitalize(key.identifier().getPath().replace("_", " "));
    }

    private static String keyToConstant(ResourceKey<?> key) {
        return key.identifier().getPath().toUpperCase(Locale.ROOT);
    }
}
