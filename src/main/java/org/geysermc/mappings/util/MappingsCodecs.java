package org.geysermc.mappings.util;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapEncoder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/// Holds various utility codecs.
public final class MappingsCodecs {
    /// Discards the namespace when encoding the {@link Identifier}, if it is `minecraft`.
    public static final Codec<Identifier> TRIMMED_IDENTIFIER_CODEC = Codec.of(Codec.STRING.comap(Identifier::toShortString), Identifier.CODEC);
    /// Discards the namespace when encoding the {@link SoundEvent}, if it is `minecraft`.
    public static final Codec<SoundEvent> TRIMMED_SOUND_EVENT_CODEC = TRIMMED_IDENTIFIER_CODEC.flatXmap(identifier -> BuiltInRegistries.SOUND_EVENT.get(identifier)
                    .map(Holder::value)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown sound event: " + identifier)),
            soundEvent -> BuiltInRegistries.SOUND_EVENT.getResourceKey(soundEvent)
                    .map(ResourceKey::identifier)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unregistered sound event: " + soundEvent)));
    /// Encodes/decodes the {@link BlockState} using the same format used in Minecraft's `/setblock` command.
    public static final Codec<BlockState> BLOCK_STATE_STRING_CODEC = Codec.STRING.comapFlatMap(MappingsUtil::blockStateFromString, MappingsUtil::blockStateToString);
    /// Preferably don't use this codec - rather, use codecs of proper types and encode these instead
    public static final Codec<JsonElement> JSON_ELEMENT = Codec.PASSTHROUGH.xmap(dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
        element -> new Dynamic<>(JsonOps.INSTANCE, element.deepCopy()));

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
