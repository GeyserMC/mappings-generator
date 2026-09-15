package org.geysermc.mappings;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.geysermc.mappings.generator.BiomeMappingsGenerator;
import org.geysermc.mappings.generator.BlockMappingsGenerator;
import org.geysermc.mappings.generator.ReadMeGenerator;
import org.geysermc.mappings.generator.shape.BlockShapeMappingsGenerator;
import org.geysermc.mappings.generator.shape.CollisionShapeMappingsGenerator;
import org.geysermc.mappings.generator.ItemDataComponentGenerator;
import org.geysermc.mappings.generator.InteractionsGenerator;
import org.geysermc.mappings.generator.ItemComponentsGenerator;
import org.geysermc.mappings.generator.ItemMappingsGenerator;
import org.geysermc.mappings.generator.MappingsGenerator;
import org.geysermc.mappings.generator.ParticleMappingsGenerator;
import org.geysermc.mappings.generator.ResolvableItemDataComponentsGenerator;
import org.geysermc.mappings.generator.SoundMappingsGenerator;
import org.geysermc.mappings.generator.UtilMappingsGenerator;
import org.geysermc.mappings.generator.javaclass.BlockStatePropertiesGenerator;
import org.geysermc.mappings.generator.javaclass.BlocksGenerator;
import org.geysermc.mappings.generator.javaclass.GameRulesGenerator;
import org.geysermc.mappings.generator.javaclass.ItemsGenerator;
import org.geysermc.mappings.generator.javaclass.MapColorsGenerator;
import org.geysermc.mappings.generator.javaclass.TagListGenerator;
import org.geysermc.mappings.generator.mcpl.BuiltinSoundGenerator;
import org.geysermc.mappings.generator.mcpl.ClientboundBlockEventPacketGenerator;
import org.geysermc.mappings.generator.mcpl.CustomStatisticGenerator;
import org.geysermc.mappings.generator.mcpl.LevelEventTypeGenerator;
import org.geysermc.mappings.generator.mcpl.NetworkCodecGenerator;
import org.geysermc.mappings.generator.mcpl.NetworkTagsGenerator;
import org.geysermc.mappings.mixin.accessor.PackGeneratorAccessor;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.util.RegistryAccessUtil;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/// The main entrypoint. {@link MappingsGenerator}s must be registered here, in the static initialiser block.
///
/// Generators must be assigned to a provider group, and can be assigned to one group *only*.
/// Other than registering new generators, you generally don't need to do much here.
///
/// Please note that {@link TagListGenerator}s are registered over at {@link TagListGenerator#addProviders(Consumer)}.
///
/// @see MappingsGenerator
public final class MappingsGenerators implements DataGeneratorEntrypoint {
    public static final String MOD_ID = "mappings-generator";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SELECTED_PROVIDER_GROUP = System.getProperty("geyser.providers.selected");

    private static final Map<String, List<ProviderFactory<? extends DataProvider>>> providerGroups = new Object2ObjectLinkedOpenHashMap<>();

    static {
        // These are sorted according to their respective FileType, please keep it that way!!

        registerProviderGroup("mcpl", builder -> builder
                .withFactory(ClientboundBlockEventPacketGenerator::new)
                .withFactory(BuiltinSoundGenerator::new)
                .withFactory(CustomStatisticGenerator::new)
                .withFactory(LevelEventTypeGenerator::new)
                .withRegistryFactory(NetworkCodecGenerator::new)
                .withRegistryFactory(NetworkTagsGenerator::new));

        registerProviderGroup("javaclass", builder -> {
            builder.withRegistryFactory(BlocksGenerator::new)
                    .withFactory(BlockStatePropertiesGenerator::new)
                    .withFactory(GameRulesGenerator::new)
                    .withFactory(MapColorsGenerator::new)
                    .withRegistryFactory(ItemsGenerator::new);
            TagListGenerator.addProviders(builder::withFactory);
            return builder;
        });

        registerProviderGroup("mappings", builder -> builder
                .withRegistryAndSamplesFactory(BiomeMappingsGenerator::new)
                .withSamplesFactory(BlockMappingsGenerator::new)
                .withFactory(BlockShapeMappingsGenerator::new)
                .withFactory(CollisionShapeMappingsGenerator::new)
                .withFactory(InteractionsGenerator::new)
                .withSamplesFactory(ItemComponentsGenerator::new)
                .withRegistryFactory(ItemDataComponentGenerator::new)
                .withSamplesFactory(ItemMappingsGenerator::new)
                .withSamplesFactory(ParticleMappingsGenerator::new)
                .withFactory(ReadMeGenerator::new)
                .withRegistryFactory(ResolvableItemDataComponentsGenerator::new)
                .withSamplesFactory(SoundMappingsGenerator::new)
                .withFactory(UtilMappingsGenerator::new));
    }

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        Path mappingsDirectory = ((PackGeneratorAccessor) (Object) pack).getOutput().getOutputFolder().resolve("mappings");
        if (!Files.isDirectory(mappingsDirectory)) {
            LOGGER.info("mappings directory not found, attempting to clone the submodule automatically...");
            // Try to clone submodules automatically
            try {
                Process process = new ProcessBuilder("git", "submodule", "update", "--init", "--recursive").start().onExit().join();
                if (process.exitValue() != 0) {
                    throw new RuntimeException("Trying to automatically clone submodules exited with exit value: " + process.exitValue());
                }
            } catch (Exception exception) {
                throw new IllegalStateException("mappings directory not found and automatically cloning the submodule failed, please clone it manually", exception);
            }

            if (!Files.exists(mappingsDirectory)) {
                throw new IllegalStateException("mappings directory still doesn't exist after cloning submodules");
            }
        }

        CompletableFuture<BedrockSamples> bedrockSamples = BedrockSamples.load(((PackGeneratorAccessor) (Object) pack).getOutput().getOutputFolder());
        // Have to use this instead of the registries provided by Fabric because these have tags
        CompletableFuture<RegistryAccess> registryAccess = RegistryAccessUtil.create();

        List<ProviderFactory<? extends DataProvider>> factories;
        if (SELECTED_PROVIDER_GROUP != null) {
            factories = providerGroups.get(SELECTED_PROVIDER_GROUP);
            if (factories == null) {
                throw new RuntimeException("Unknown provider group\"" + SELECTED_PROVIDER_GROUP + "\"");
            }
            LOGGER.info("Running provider group \"{}\"", SELECTED_PROVIDER_GROUP);
        } else {
            factories = providerGroups.entrySet().stream().flatMap(entry -> entry.getValue().stream()).toList();
            LOGGER.info("Running all providers");
        }

        // Java stuff
        factories.forEach(provider -> pack.addProvider((FabricDataGenerator.Pack.Factory<? extends DataProvider>)output -> provider.create(output, registryAccess, bedrockSamples)));
    }

    private static void registerProviderGroup(String name, UnaryOperator<ProviderGroupBuilder> builder) {
        if (providerGroups.containsKey(name)) {
            throw new IllegalArgumentException("Tried to register provider group for name " + name + " twice!");
        }
        providerGroups.put(name, builder.apply(new ProviderGroupBuilder()).factories);
    }

    private static class ProviderGroupBuilder {
        private final List<ProviderFactory<? extends DataProvider>> factories = new ArrayList<>();

        private ProviderGroupBuilder withFactory(Function<PackOutput, ? extends DataProvider> factory) {
            return withRegistryAndSamplesFactory((output, _, _) -> factory.apply(output));
        }

        private ProviderGroupBuilder withRegistryFactory(BiFunction<PackOutput, CompletableFuture<RegistryAccess>, ? extends DataProvider> factory) {
            return withRegistryAndSamplesFactory((output, registries, _) -> factory.apply(output, registries));
        }

        private ProviderGroupBuilder withSamplesFactory(BiFunction<PackOutput, CompletableFuture<BedrockSamples>, ? extends DataProvider> factory) {
            return withRegistryAndSamplesFactory((output, _, samples) -> factory.apply(output, samples));
        }

        private ProviderGroupBuilder withRegistryAndSamplesFactory(ProviderFactory<? extends DataProvider> factory) {
            this.factories.add(factory);
            return this;
        }
    }

    private interface ProviderFactory<T extends DataProvider> {

        T create(PackOutput output, CompletableFuture<RegistryAccess> registries, CompletableFuture<BedrockSamples> samples);
    }
}
