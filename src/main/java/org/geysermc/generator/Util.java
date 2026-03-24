package org.geysermc.generator;

import lombok.SneakyThrows;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.PrintStream;
import java.util.List;
import java.util.stream.Stream;

public class Util {
    public static RegistryAccess.Frozen registryAccess;

    public static void initialize() {
        PrintStream err = System.err;
        PrintStream out = System.out;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        // Revert this stupid thing that the Bootstrap process does
        System.setErr(err);
        System.setOut(out);

        registryAccess = registryAccess();
    }

    @SneakyThrows
    private static RegistryAccess.Frozen registryAccess() {
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
        worldgenAccess = RegistryDataLoader.load(resourceManager, worldgenLookups, RegistryDataLoader.WORLDGEN_REGISTRIES, net.minecraft.util.Util.backgroundExecutor()).get();

        List<HolderLookup.RegistryLookup<?>> worldgenRegistries = Stream.concat(worldgenLookups.stream(), worldgenAccess.listRegistries()).toList();
        RegistryAccess.Frozen dimensionsAccess = RegistryDataLoader.load(resourceManager, worldgenRegistries, RegistryDataLoader.DIMENSION_REGISTRIES, net.minecraft.util.Util.backgroundExecutor()).get();

        HolderLookup.Provider worldgenLookupProvider = HolderLookup.Provider.create(worldgenRegistries.stream());
        WorldDimensions dimensions = WorldPresets.createNormalWorldDimensions(worldgenLookupProvider);
        WorldDimensions.Complete complete = dimensions.bake(dimensionsAccess.lookupOrThrow(Registries.LEVEL_STEM));
        LayeredRegistryAccess<RegistryLayer> registryAccess = initialRegistryAccess.replaceFrom(
                RegistryLayer.WORLDGEN, worldgenAccess, complete.dimensionsRegistryAccess()
        );

        pendingTags.forEach(Registry.PendingTags::apply);

        return registryAccess.compositeAccess();
    }
}
