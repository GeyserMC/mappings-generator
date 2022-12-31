package org.geysermc.generator;

import com.mojang.serialization.DataResult;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryDataLoader;
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