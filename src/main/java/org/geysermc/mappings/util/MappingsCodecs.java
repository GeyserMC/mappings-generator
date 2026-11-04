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
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MappingsCodecs {
    public static final Codec<Identifier> TRIMMED_IDENTIFIER_CODEC = Codec.of(Codec.STRING.comap(Identifier::toShortString), Identifier.CODEC);
    public static final Codec<SoundEvent> TRIMMED_SOUND_EVENT_CODEC = TRIMMED_IDENTIFIER_CODEC.flatXmap(identifier -> BuiltInRegistries.SOUND_EVENT.get(identifier)
                    .map(Holder::value)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown sound event: " + identifier)),
            soundEvent -> BuiltInRegistries.SOUND_EVENT.getResourceKey(soundEvent)
                    .map(ResourceKey::identifier)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unregistered sound event: " + soundEvent)));
    public static final Codec<BlockState> BLOCK_STATE_STRING_CODEC = Codec.STRING.comapFlatMap(MappingsUtil::blockStateFromString, MappingsUtil::blockStateToString);
    /// Preferably don't use this codec - rather, use codecs of proper types and encode these instead
    public static final Codec<JsonElement> JSON_ELEMENT = Codec.PASSTHROUGH.xmap(dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
        element -> new Dynamic<>(JsonOps.INSTANCE, element.deepCopy()));

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
