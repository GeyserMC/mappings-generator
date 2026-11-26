package org.geysermc.mappings.util;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapEncoder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

/// Holds various utility codecs.
public final class MappingsCodecs {
    /// Encodes/decodes the {@link BlockState} using the same format used in Minecraft's `/setblock` command.
    public static final Codec<BlockState> BLOCK_STATE_STRING_CODEC = Codec.STRING.comapFlatMap(MappingsUtil::blockStateFromString, MappingsUtil::blockStateToString);
    /// Preferably don't use this codec - rather, use codecs of proper types and encode these instead
    public static final Codec<JsonElement> JSON_ELEMENT = Codec.PASSTHROUGH.xmap(dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
        element -> new Dynamic<>(JsonOps.INSTANCE, element.deepCopy()));
    /// A codec that parses/writes {@link Properties} to and from a {@link String}.
    public static final Codec<Properties> PROPERTIES = Codec.STRING.comapFlatMap(string -> {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(string));
        } catch (Exception exception) {
            return DataResult.error(() -> "Failed to parse properties: " + exception.getMessage());
        }
        return DataResult.success(properties);
    }, properties -> {
        StringWriter writer = new StringWriter();
        try {
            properties.store(writer, null);
        } catch (IOException exception) {
            // Should never happen since we're working with StringWriter
            throw new UncheckedIOException(exception);
        }
        return writer.toString();
    });
    /// Encodes/decodes Cloudburst's {@link LevelEvent} enum.
    public static final Codec<LevelEvent> LEVEL_EVENT = MappingsCodecs.enumCodec(LevelEvent::values, LevelEvent::name);

    /// Creates a codec for an enum or enum-like structure. Prefer to use {@link StringRepresentable} when possible,
    /// this method should only be used for enums in libraries.
    ///
    /// @param valuesSupplier a supplier supplying the values of the enum
    /// @param toString a method converting an enum value {@link E} to its string representation
    /// @param <E> the enum type
    public static <E> Codec<E> enumCodec(Supplier<E[]> valuesSupplier, Function<E, String> toString) {
        E[] values = valuesSupplier.get();
        //noinspection NullableProblems - Mojang messed up the annotations
        return Codec.stringResolver(toString, string -> {
            for (E value : values) {
                if (toString.apply(value).equals(string)) {
                    return value;
                }
            }
            return null;
        });
    }

    /// Creates a "fuzzy codec", a {@link MapCodec} capable of encoding a sealed interface {@link T}, by
    /// attempting each of the given codecs when decoding, and using the `encoderGetter` when encoding.
    ///
    /// @param codecs the codecs for each of the implementations of {@link T}
    /// @param encoderGetter a method that gets a codec from an implementation of {@link T}. Usually this is implemented by adding a `codec()` method to the interface
    /// @param <T> the type to encode
    @SuppressWarnings("unchecked")
    public static <T> MapCodec<T> fuzzyCodec(Collection<MapCodec<? extends T>> codecs,
                                             Function<T, ? extends MapEncoder<? extends T>> encoderGetter) {
        // Can't use a mixin with invoker here, because invokers require the exact return type
        try {
            Class<?> codecClazz = Class.forName("net.minecraft.network.chat.ComponentSerialization$FuzzyCodec");
            Constructor<?> constructor = codecClazz.getDeclaredConstructor(Collection.class, Function.class);
            constructor.setAccessible(true);
            return (MapCodec<T>) constructor.newInstance(codecs, encoderGetter);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private MappingsCodecs() {}
}
