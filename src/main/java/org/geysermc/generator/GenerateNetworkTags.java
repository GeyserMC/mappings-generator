package org.geysermc.generator;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateNetworkTags {
    private static final CompoundTag tags = new CompoundTag();

    public static void main(String[] args) {
        Util.initialize();

        PackRepository packRepository = ServerPacksSource.createVanillaTrustedRepository();
        packRepository.reload();
        packRepository.setSelected(packRepository.getAvailableIds());

        CloseableResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA, packRepository.openAllSelected());
        LayeredRegistryAccess<RegistryLayer> initialRegistryAccess = RegistryLayer.createRegistryAccess();
        List<Registry.PendingTags<?>> pendingTags = TagLoader.loadTagsForExistingRegistries(
                resourceManager, initialRegistryAccess.getLayer(RegistryLayer.STATIC)
        );

        RegistryAccess.Frozen worldgenAccess = initialRegistryAccess.getAccessForLoading(RegistryLayer.WORLDGEN);
        List<HolderLookup.RegistryLookup<?>> worldgenLookups = TagLoader.buildUpdatedLookups(worldgenAccess, pendingTags);
        worldgenAccess = RegistryDataLoader.load(resourceManager, worldgenLookups, RegistryDataLoader.WORLDGEN_REGISTRIES);

        List<HolderLookup.RegistryLookup<?>> worldgenRegistries = Stream.concat(worldgenLookups.stream(), worldgenAccess.listRegistries()).toList();
        RegistryAccess.Frozen dimensionsAccess = RegistryDataLoader.load(resourceManager, worldgenRegistries, RegistryDataLoader.DIMENSION_REGISTRIES);

        HolderLookup.Provider worldgenLookupProvider = HolderLookup.Provider.create(worldgenRegistries.stream());
        WorldDimensions dimensions = WorldPresets.createNormalWorldDimensions(worldgenLookupProvider);
        WorldDimensions.Complete complete = dimensions.bake(dimensionsAccess.lookupOrThrow(Registries.LEVEL_STEM));
        LayeredRegistryAccess<RegistryLayer> registryAccess = initialRegistryAccess.replaceFrom(
                RegistryLayer.WORLDGEN, worldgenAccess, complete.dimensionsRegistryAccess()
        );

        pendingTags.forEach(Registry.PendingTags::apply);

        Map<ResourceKey<?>, Map<ResourceLocation, IntList>> tagMap = RegistrySynchronization.networkSafeRegistries(registryAccess)
                .map(registryEntry -> Pair.of(registryEntry.key(), serializeToNetwork(registryEntry.value())))
                .filter(pair -> !pair.getSecond().isEmpty())
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        for (Map.Entry<ResourceKey<?>, Map<ResourceLocation, IntList>> registry : tagMap.entrySet()) {
            CompoundTag temp = new CompoundTag();
            for (Map.Entry<ResourceLocation, IntList> tag : registry.getValue().entrySet()) {
                temp.put(tag.getKey().toString(), new IntArrayTag(tag.getValue().toIntArray()));
            }
            tags.put(registry.getKey().location().toString(), temp);
        }

        try {
            NbtIo.writeCompressed(tags, Path.of("./tags.nbt"));
            System.out.println("Finished writing tags.nbt!");
        } catch (IOException e) {
            System.out.println("Failed to write tags.nbt!");
            e.printStackTrace();
        }
    }

    private static <T> Map<ResourceLocation, IntList> serializeToNetwork(Registry<T> registry) {
        Map<ResourceLocation, IntList> map = new HashMap<>();
        registry.getTags().forEach(named -> {
            IntList intList = new IntArrayList(named.size());
            for (Holder<T> holder : named) {
                if (holder.kind() != Holder.Kind.REFERENCE) {
                    throw new IllegalStateException("Can't serialize unregistered value " + holder);
                }
                intList.add(registry.getId(holder.value()));
            }
            map.put(named.key().location(), intList);
        });
        return map;
    }
}
