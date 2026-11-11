package org.geysermc.mappings;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.geysermc.mappings.generator.MappingsGenerator;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/// Interface representing access to a root directory in a file system, such as the `mappings` directory or the root
/// of `bedrock-samples.zip`. Has helper methods for reading and writing files using a {@link FileType}.
///
/// There are also variants of these helper methods for {@link FileType}s with codecs that require {@link RegistryAccess} - generally,
/// you'll figure out easily if this is the case. When in doubt, use the methods without a {@link RegistryAccess} parameter, and if it throws,
/// try the ones with. Read the documentation over at {@link MappingsGenerator} for obtaining a {@link RegistryAccess} instance.
///
/// Each file touched by the generator, be it through reading, writing, or both, must have an accompanying {@link FileType}, and must be interacted through using
/// {@link FileType}s in combination with a {@link FileSystemAccess}. {@link MappingsGenerator} implements this interface,
/// and as such you can make use of all the methods this interface provides in any generator.
///
/// @see MappingsGenerator
/// @see FileSystemAccess#saveFile(CachedOutput, FileType, Object)
/// @see FileSystemAccess#saveFile(CachedOutput, RegistryAccess, FileType, Object)
/// @see FileSystemAccess#readFileOrThrow(FileType)
/// @see FileSystemAccess#readFile(FileType)
/// @see FileSystemAccess#readFileOrThrow(FileType, RegistryAccess)
/// @see FileSystemAccess#readFile(FileType, RegistryAccess)
@FunctionalInterface
public interface FileSystemAccess {

    Logger LOGGER = LogUtils.getLogger();

    /// When set to true, writes all NBT files as SNBT ones.
    ///
    /// Be careful with this option - if any {@link DataProvider}s require reading the file back, they may fail on the next run.
    /// 
    /// It might be preferred to change the {@link FileType} of a single file to JSON instead, which usually shouldn't cause many problems, unless the {@link DataProvider} using it requires reading the file back.
    boolean NBT_DEBUG_MODE = false;

    /// {@link FileType.Type#TEXT} files are serialised by encoding with a {@link JavaOps} and then calling {@link Object#toString()} - generally this works
    /// best if the type of {@link FileType} is {@link String} (and the codec is {@link Codec#STRING}).
    ///
    /// @return a {@link CompletableFuture} that writes the {@code value} for the given {@link FileType} to the {@link CachedOutput} in the root folder
    /// @see FileSystemAccess#saveFile(CachedOutput, RegistryAccess, FileType, Object)
    default <T> CompletableFuture<?> saveFile(CachedOutput cache, FileType<T> type, T value) {
        checkFileTypeForSaving(type);
        return switch (type.type()) {
            case JSON -> saveJsonFile(cache, path(type), type.codec(), value);
            case NBT -> saveNbtFile(cache, path(type), type.codec(), value);
            case TEXT -> saveTextFile(cache, path(type), type.codec(), value);
        };
    }

    /// Use this method for {@link FileType}s with codecs that require {@link RegistryAccess}.
    ///
    /// @return a {@link CompletableFuture} that writes the {@code value} for the given {@link FileType} to the {@link CachedOutput} in the root folder
    /// @see FileSystemAccess#saveFile(CachedOutput, FileType, Object)
    default <T> CompletableFuture<?> saveFile(CachedOutput cache, RegistryAccess registries, FileType<T> type, T value) {
        checkFileTypeForSaving(type);
        return switch (type.type()) {
            case JSON -> saveJsonFile(cache, path(type), registries, type.codec(), value);
            case NBT -> saveNbtFile(cache, path(type), registries, type.codec(), value);
            case TEXT -> throw new UnsupportedOperationException("Unable to save text files with registry data!");
        };
    }

    /// @return a {@link CompletableFuture} that reads the {@link FileType} in the root folder, or throws if it didn't exist
    /// @see FileSystemAccess#readFile(FileType)
    /// @see FileSystemAccess#readFile(FileType, RegistryAccess)
    default <T> CompletableFuture<T> readFileOrThrow(FileType<T> type) {
        return readFile(type).thenApply(optional -> optional.orElseThrow(() -> new IllegalStateException("Missing required file " + type.path())));
    }

    /// {@link FileType.Type#TEXT} files are parsed by reading the file as a string, and passing this to a {@link JavaOps} - generally this works
    /// best if the type of {@link FileType} is {@link String} (and the codec is {@link Codec#STRING}).
    ///
    /// @return a {@link CompletableFuture} that reads the {@link FileType} in the root folder, or returns an empty {@link Optional} if it didn't exist
    /// @see FileSystemAccess#readFileOrThrow(FileType)
    /// @see FileSystemAccess#readFile(FileType, RegistryAccess)
    default <T> CompletableFuture<Optional<T>> readFile(FileType<T> type) {
        return switch (type.type()) {
            case JSON -> readJsonFile(path(type), type.codec());
            case NBT -> readNbtFile(path(type), type.codec());
            case TEXT -> readTextFile(path(type), type.codec());
        };
    }

    /// Use this method for {@link FileType}s with codecs that require {@link RegistryAccess}.
    ///
    /// @return a {@link CompletableFuture} that reads the {@link FileType} in the root folder, or throws if it didn't exist
    /// @see FileSystemAccess#readFile(FileType, RegistryAccess)
    /// @see FileSystemAccess#readFileOrThrow(FileType)
    default <T> CompletableFuture<T> readFileOrThrow(FileType<T> type, RegistryAccess registries) {
        return readFile(type, registries).thenApply(optional -> optional.orElseThrow(() -> new IllegalStateException("Missing required file " + type.path())));
    }

    /// Use this method for {@link FileType}s with codecs that require {@link RegistryAccess}.
    ///
    /// @return a {@link CompletableFuture} that reads the {@link FileType} in the root folder, or returns an empty {@link Optional} if it didn't exist
    /// @see FileSystemAccess#readFileOrThrow(FileType, RegistryAccess)
    /// @see FileSystemAccess#readFile(FileType)
    default <T> CompletableFuture<Optional<T>> readFile(FileType<T> type, RegistryAccess registries) {
        return switch (type.type()) {
            case JSON -> readJsonFile(path(type), registries, type.codec());
            case NBT -> throw new UnsupportedOperationException("Unable to read NBT files with registry data!");
            case TEXT -> throw new UnsupportedOperationException("Unable to read text files!");
        };
    }

    /// @return the path of the given {@link FileType} in the {@link FileSystemAccess#root()}
    default Path path(FileType<?> type) {
        return root().resolve(type.path());
    }

    /// @return the root folder of this file system, which all used {@link FileType}s will be resolved to
    Path root();

    static <T> CompletableFuture<?> saveTextFile(CachedOutput output, Path file, Codec<T> codec, T value) {
        return saveFile(output, file, (object, stream) -> stream.write(object.toString().getBytes(StandardCharsets.UTF_8)), JavaOps.INSTANCE, codec, value);
    }

    static <T> CompletableFuture<Optional<T>> readTextFile(Path file, Codec<T> codec) {
        return readFile(file, Files::readString, JavaOps.INSTANCE, codec);
    }

    static <T> CompletableFuture<?> saveNbtFile(CachedOutput output, Path file, Codec<T> codec, T value) {
        return saveNbtFile(output, file, NbtOps.INSTANCE, codec, value);
    }

    static <T> CompletableFuture<?> saveNbtFile(CachedOutput output, Path file, RegistryAccess registries, Codec<T> codec, T value) {
        return saveNbtFile(output, file, registries.createSerializationContext(NbtOps.INSTANCE), codec, value);
    }

    static <T> CompletableFuture<?> saveNbtFile(CachedOutput output, Path file, DynamicOps<Tag> ops, Codec<T> codec, T value) {
        return saveFile(output, file, (tag, stream) -> {
            if (NBT_DEBUG_MODE) {
                stream.write(new SnbtPrinterTagVisitor().visit(tag).getBytes(StandardCharsets.UTF_8));
            } else {
                NbtIo.writeCompressed((CompoundTag) tag, stream);
            }
        }, ops, codec, value);
    }

    static <T> CompletableFuture<Optional<T>> readNbtFile(Path file, Codec<T> codec) {
        return readNbtFile(file, NbtOps.INSTANCE, codec);
    }

    static <T> CompletableFuture<Optional<T>> readNbtFile(Path file, DynamicOps<Tag> ops, Codec<T> codec) {
        return readFile(file, _ -> NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), ops, codec);
    }

    static <T> CompletableFuture<?> saveJsonFile(CachedOutput output, Path file, Codec<T> codec, T value) {
        return saveJsonFile(output, file, JsonOps.INSTANCE, codec, value);
    }

    static <T> CompletableFuture<?> saveJsonFile(CachedOutput output, Path file, RegistryAccess registries, Codec<T> codec, T value) {
        return saveJsonFile(output, file, registries.createSerializationContext(JsonOps.INSTANCE), codec, value);
    }

    // Inspired by DataProvider#saveStable
    static <T> CompletableFuture<?> saveJsonFile(CachedOutput output, Path file, DynamicOps<JsonElement> ops, Codec<T> codec, T value) {
        return saveFile(output, file, (json, stream) -> {
            try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
                writer.setSerializeNulls(false);
                writer.setIndent("  ");
                GsonHelper.writeValue(writer, json, DataProvider.KEY_COMPARATOR);
            }
        }, ops, codec, value);
    }

    static <T> CompletableFuture<Optional<T>> readJsonFile(Path file, Codec<T> codec) {
        return readJsonFile(file, JsonOps.INSTANCE, codec);
    }

    static <T> CompletableFuture<Optional<T>> readJsonFile(Path file, RegistryAccess registries, Codec<T> codec) {
        return readJsonFile(file, registries.createSerializationContext(JsonOps.INSTANCE), codec);
    }

    static <T> CompletableFuture<Optional<T>> readJsonFile(Path file, DynamicOps<JsonElement> ops, Codec<T> codec) {
        return readFile(file, _ -> JsonParser.parseString(Files.readString(file)), ops, codec);
    }

    // Inspired by DataProvider#saveStable
    static <O, T> CompletableFuture<?> saveFile(CachedOutput cache, Path path, IOWriter<O> writer, DynamicOps<O> ops, Codec<T> codec, T value) {
        return CompletableFuture.runAsync(() -> {
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                try (HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha1(), bytes)) {
                    writer.write(codec.encodeStart(ops, value).getOrThrow(), hashedBytes);
                    cache.writeIfNeeded(path, bytes.toByteArray(), hashedBytes.hash());
                }
            } catch (IOException exception) {
                LOGGER.error("Failed to save file to {}", path, exception);
            }
        }, Util.backgroundExecutor().forName("FileSystemAccess#saveFile"));
    }

    static <O, T> CompletableFuture<Optional<T>> readFile(Path file, IOReader<O> reader, DynamicOps<O> ops, Codec<T> codec) {
        if (!Files.exists(file)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Optional.of(codec.parse(ops, reader.read(file)).getOrThrow());
            } catch (IOException exception) {
                throw new UncheckedIOException("IOException occurred whilst reading file", exception);
            }
        }, Util.backgroundExecutor().forName("FileSystemAccess#readFile"));
    }

    private static void checkFileTypeForSaving(FileType<?> type) {
        if (!type.managed()) {
            throw new IllegalArgumentException("Unable to save data to " + type.path() + " because it is not managed by us");
        }
    }

    @FunctionalInterface
    interface IOReader<T> {

        T read(Path file) throws IOException;
    }

    @FunctionalInterface
    interface IOWriter<T> {

        void write(T value, OutputStream stream) throws IOException;
    }
}
