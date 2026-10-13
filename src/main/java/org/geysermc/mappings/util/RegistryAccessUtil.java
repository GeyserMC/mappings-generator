package org.geysermc.mappings.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class RegistryAccessUtil {

    public static CompletableFuture<RegistryAccess> create() {
        return CompletableFuture.supplyAsync(() -> {
                PackRepository packRepository = ServerPacksSource.createVanillaTrustedRepository();
                packRepository.reload();
                packRepository.setSelected(packRepository.getAvailableIds());
                return new MultiPackResourceManager(PackType.SERVER_DATA, packRepository.openAllSelected());
            }, Util.backgroundExecutor())
                .thenCompose(resourceManager -> {
                    LayeredRegistryAccess<RegistryLayer> initialRegistryAccess = RegistryLayer.createRegistryAccess();
                    List<Registry.PendingTags<?>> pendingTags = TagLoader.loadTagsForExistingRegistries(resourceManager, initialRegistryAccess.getLayer(RegistryLayer.STATIC));

                    RegistryAccess.Frozen worldgenAccess = initialRegistryAccess.getAccessForLoading(RegistryLayer.WORLD);
                    List<HolderLookup.RegistryLookup<?>> worldgenLookups = TagLoader.buildUpdatedLookups(worldgenAccess, pendingTags);
                    return RegistryDataLoader.load(resourceManager, worldgenLookups, RegistryDataLoader.WORLD_REGISTRIES, Util.backgroundExecutor())
                            .thenCompose(loadedWorldgenRegistries -> {
                                List<HolderLookup.RegistryLookup<?>> worldgenRegistries = Stream.concat(worldgenLookups.stream(), loadedWorldgenRegistries.listRegistries()).toList();
                                return RegistryDataLoader.load(resourceManager, worldgenRegistries, RegistryDataLoader.DIMENSION_REGISTRIES, Util.backgroundExecutor())
                                        .thenApply(dimensionRegistries -> {
                                            resourceManager.close();

                                            HolderLookup.Provider worldgenLookupProvider = HolderLookup.Provider.create(worldgenRegistries.stream());
                                            WorldDimensions dimensions = WorldPresets.createNormalWorldDimensions(worldgenLookupProvider);
                                            WorldDimensions.Complete complete = dimensions.bake(dimensionRegistries.lookupOrThrow(Registries.LEVEL_STEM));
                                            LayeredRegistryAccess<RegistryLayer> registryAccess = initialRegistryAccess.replaceFrom(RegistryLayer.WORLD,
                                                    loadedWorldgenRegistries, complete.dimensionsRegistryAccess());

                                            pendingTags.forEach(Registry.PendingTags::apply);

                                            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registryAccess.compositeAccess()).forEach(DataComponentInitializers.PendingComponents::apply);
                                            return registryAccess.compositeAccess();
                                        });
                            });
                });
    }

    private RegistryAccessUtil() {}
}
