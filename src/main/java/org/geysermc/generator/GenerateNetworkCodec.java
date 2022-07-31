package org.geysermc.generator;

import com.mojang.serialization.DataResult;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.io.IOException;

public class GenerateNetworkCodec {
	public static void main(String[] args) {
		Util.initialize();
		RegistryAccess.Frozen registryAccess = RegistryAccess.BUILTIN.get();

		DataResult<Tag> dataResult = RegistryAccess.NETWORK_CODEC.encodeStart(NbtOps.INSTANCE, registryAccess);
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
