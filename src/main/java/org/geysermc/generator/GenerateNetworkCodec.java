package org.geysermc.generator;

import io.netty.handler.codec.EncoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class GenerateNetworkCodec {
    public static void main(String[] args) {
        Util.initialize();

        CloseableResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, Collections.singletonList(new ServerPacksSource().getVanillaPack()));
        RegistryAccess.Frozen worldGenAccess = RegistryLayer.createRegistryAccess().getAccessForLoading(RegistryLayer.WORLDGEN);
        RegistryAccess.Frozen registryAccess = RegistryDataLoader.load(resourceManager, worldGenAccess, RegistryDataLoader.WORLDGEN_REGISTRIES);

        Tag tag = net.minecraft.Util.getOrThrow(RegistrySynchronization.NETWORK_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)), registryAccess), error -> {
            return new EncoderException("Failed to encode: " + error + " " + registryAccess);
        });

        try {
            NbtIo.writeCompressed((CompoundTag) tag, new File("./networkCodec.nbt"));
            System.out.println("Finished writing networkCodec.nbt!");
        } catch (IOException e) {
            System.out.println("Failed to write networkCodec.nbt!");
            e.printStackTrace();
        }
    }
}