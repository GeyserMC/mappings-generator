package org.geysermc.generator;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GenerateNetworkCodec {
    public static void main(String[] args) {
        Util.initialize();
        WritableRegistry<ChatType> chatTypeRegistry = new MappedRegistry<>(Registry.CHAT_TYPE_REGISTRY, Lifecycle.experimental());
        WritableRegistry<DimensionType> dimensionTypeRegistry = new MappedRegistry<>(Registry.DIMENSION_TYPE_REGISTRY, Lifecycle.experimental());
        WritableRegistry<Biome> biomeRegistry = new MappedRegistry<>(Registry.BIOME_REGISTRY, Lifecycle.experimental());
        VanillaRegistries.createLookup().lookup(Registry.CHAT_TYPE_REGISTRY).get().listElements().toList().forEach(chatType -> chatTypeRegistry.register(chatType.key(), chatType.value(), Lifecycle.experimental()));
        VanillaRegistries.createLookup().lookup(Registry.DIMENSION_TYPE_REGISTRY).get().listElements().toList().forEach(dimensionType -> dimensionTypeRegistry.register(dimensionType.key(), dimensionType.value(), Lifecycle.experimental()));
        VanillaRegistries.createLookup().lookup(Registry.BIOME_REGISTRY).get().listElements().toList().forEach(biome -> biomeRegistry.register(biome.key(), biome.value(), Lifecycle.experimental()));
        RegistryAccess registryAccess = new RegistryAccess.ImmutableRegistryAccess(List.of(chatTypeRegistry, dimensionTypeRegistry, biomeRegistry)).freeze();

        DataResult<Tag> dataResult = RegistrySynchronization.NETWORK_CODEC.encodeStart(NbtOps.INSTANCE, registryAccess);
        dataResult.error().ifPresent((action) -> {
            throw new EncoderException("Failed to encode: " + action.message() + " " + registryAccess);
        });
        CompoundTag tag = (CompoundTag) dataResult.result().get();

        try {
            NbtIo.writeCompressed(tag, new File("./networkCodec.nbt"));
            System.out.println("Finished writing networkCodec.nbt!");
        } catch (IOException e) {
            System.out.println("Failed to write networkCodec.nbt!");
            e.printStackTrace();
        }
    }
}
