package org.geysermc.mappings;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataProvider;
import org.geysermc.mappings.generator.BiomeMappingsGenerator;
import org.geysermc.mappings.generator.BlockMappingsGenerator;
import org.geysermc.mappings.generator.MapDecorationMappingsGenerator;
import org.geysermc.mappings.generator.PotPatternMappingsGenerator;
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
import org.geysermc.mappings.generator.javaclass.TagListGenerator;
import org.geysermc.mappings.generator.mcpl.ClientboundBlockEventPacketGenerator;
import org.geysermc.mappings.generator.mcpl.NetworkCodecGenerator;
import org.geysermc.mappings.generator.mcpl.NetworkTagsGenerator;
import org.geysermc.mappings.mixin.accessor.PackGeneratorAccessor;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.util.RegistryAccessUtil;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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

    private static final Map<String, ProviderGroup> providerGroups = new Object2ObjectLinkedOpenHashMap<>();

    static {
        // These are sorted according to their respective FileType, please keep it that way!!

        registerProviderGroup("mcpl", builder -> builder
                .with(EnumDataGenerators::mcpl)
                .withFactory(ClientboundBlockEventPacketGenerator::new)
                .withRegistryFactory(NetworkCodecGenerator::new)
                .withRegistryFactory(NetworkTagsGenerator::new));

        registerProviderGroup("javaclass", builder -> {
            builder.with(EnumDataGenerators::enums)
                    .withRegistryFactory(BlocksGenerator::new)
                    .withFactory(BlockStatePropertiesGenerator::new)
                    .withFactory(GameRulesGenerator::new)
                    .withRegistryFactory(ItemsGenerator::new);
            TagListGenerator.addProviders(builder::withFactory);
            return builder;
        });

        registerProviderGroup("mappings", builder -> builder
                .withRegistryAndSamplesFactory(BiomeMappingsGenerator::new)
                .withSamplesFactory(BlockMappingsGenerator::new)
                .withFactory(BlockShapeMappingsGenerator::new)
                .withFactory(CollisionShapeMappingsGenerator::new)
                .withRegistryFactory(PotPatternMappingsGenerator::new)
                .withFactory(InteractionsGenerator::new)
                .withSamplesFactory(ItemComponentsGenerator::new)
                .withRegistryFactory(ItemDataComponentGenerator::new)
                .withSamplesFactory(ItemMappingsGenerator::new)
                .withSamplesFactory(MapDecorationMappingsGenerator::new)
                .withSamplesFactory(ParticleMappingsGenerator::new)
                .withFactory(ReadMeGenerator::new)
                .withRegistryFactory(ResolvableItemDataComponentsGenerator::new)
                .withSamplesFactory(SoundMappingsGenerator::new)
                .withFactory(UtilMappingsGenerator::new));
    }

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        ProviderGroup providers;
        if (SELECTED_PROVIDER_GROUP != null) {
            providers = providerGroups.get(SELECTED_PROVIDER_GROUP);
            if (providers == null) {
                throw new RuntimeException("Unknown provider group\"" + SELECTED_PROVIDER_GROUP + "\"");
            }
            LOGGER.info("Running provider group \"{}\"", SELECTED_PROVIDER_GROUP);
        } else {
            providers = providerGroups.values().stream().reduce(ProviderGroup::merge).orElseThrow();
            LOGGER.info("Running all providers");
        }

        // Have to use this instead of the registries provided by Fabric because these have tags
        CompletableFuture<RegistryAccess> registryAccess = providers.requiresRegistries() ? RegistryAccessUtil.create() : null;
        CompletableFuture<BedrockSamples> bedrockSamples = providers.requiresSamples()
                ? BedrockSamples.load(((PackGeneratorAccessor) (Object) pack).getOutput().getOutputFolder())
                : null;

        // Java stuff
        providers.factories()
                .forEach(provider -> pack.addProvider((FabricDataGenerator.Pack.Factory<? extends DataProvider>)output -> provider.create(output, registryAccess, bedrockSamples)));
    }

    private static void registerProviderGroup(String name, UnaryOperator<ProviderGroup.Builder> builder) {
        if (providerGroups.containsKey(name)) {
            throw new IllegalArgumentException("Tried to register provider group for name " + name + " twice!");
        }
        providerGroups.put(name, ProviderGroup.of(builder));
    }
}
