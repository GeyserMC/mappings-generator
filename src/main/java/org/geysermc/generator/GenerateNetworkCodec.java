package org.geysermc.generator;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GenerateNetworkCodec {
    private static final CompoundTag networkCodec = new CompoundTag();

    public static void main(String[] args) throws URISyntaxException, IOException {
        Util.initialize();

        PackRepository packRepository = ServerPacksSource.createVanillaTrustedRepository();
        packRepository.reload();
        packRepository.setSelected(packRepository.getAvailableIds());

        CloseableResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, packRepository.openAllSelected());
        List<Registry.PendingTags<?>> list = TagLoader.loadTagsForExistingRegistries(resourceManager, RegistryLayer.createRegistryAccess().getLayer(RegistryLayer.STATIC));
        RegistryAccess.Frozen worldGenAccess = RegistryLayer.createRegistryAccess().getAccessForLoading(RegistryLayer.WORLDGEN);
        RegistryAccess.Frozen loaded = RegistryDataLoader.load(resourceManager, TagLoader.buildUpdatedLookups(worldGenAccess, list), RegistryDataLoader.WORLDGEN_REGISTRIES);
        LayeredRegistryAccess<RegistryLayer> registryAccess = RegistryLayer.createRegistryAccess().replaceFrom(RegistryLayer.WORLDGEN, loaded);
        DynamicOps<Tag> dynamicOps = registryAccess.compositeAccess().createSerializationContext(NbtOps.INSTANCE);

        RegistryDataLoader.SYNCHRONIZED_REGISTRIES.forEach(registryData -> nextStep(dynamicOps, registryData, registryAccess.getAccessFrom(RegistryLayer.WORLDGEN)));

        try {
            NbtIo.writeCompressed(networkCodec, Path.of("./networkCodec.nbt"));
            System.out.println("Finished writing networkCodec.nbt!");
        } catch (IOException e) {
            System.out.println("Failed to write networkCodec.nbt!");
            e.printStackTrace();
        }
    }

    static <T> void nextStep(DynamicOps<Tag> dynamicOps, RegistryDataLoader.RegistryData<T> registryData, RegistryAccess registryAccess) {
        registryAccess.lookup(registryData.key())
            .ifPresent(
                registry -> {
                    List<RegistrySynchronization.PackedRegistryEntry> list = new ArrayList<>(registry.size());
                    registry.listElements()
                        .forEach(
                            reference -> {
                                Tag tag = registryData.elementCodec().encodeStart(dynamicOps, reference.value())
                                        .getOrThrow(string -> new IllegalArgumentException("Failed to encode: " + reference.key() + ": " + string));
                                list.add(new RegistrySynchronization.PackedRegistryEntry(reference.key().location(), Optional.of(tag)));
                            }
                        );

                    // Credit ViaBackwards: https://github.com/ViaVersion/ViaBackwards/blob/dev/common/src/main/java/com/viaversion/viabackwards/protocol/protocol1_20_3to1_20_5/rewriter/EntityPacketRewriter1_20_5.java
                    final CompoundTag registryTag = new CompoundTag();
                    final ListTag entriesTag = new ListTag();
                    registryTag.putString("type", registry.key().location().toString());
                    registryTag.put("value", entriesTag);
                    for (int i = 0; i < list.size(); i++) {
                        final RegistrySynchronization.PackedRegistryEntry entry = list.get(i);
                        final CompoundTag entryCompoundTag = new CompoundTag();
                        entryCompoundTag.putString("name", entry.id().toString());
                        entryCompoundTag.putInt("id", i);
                        entryCompoundTag.put("element", entry.data().get());
                        entriesTag.add(entryCompoundTag);
                    }

                    networkCodec.put(registry.key().location().toString(), registryTag);
                }
            );
    }
}