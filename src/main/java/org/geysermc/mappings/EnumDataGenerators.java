package org.geysermc.mappings;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import org.geysermc.mappings.generator.javaclass.EnumDataGenerator;
import org.geysermc.mappings.generator.javaclass.MapColorsGenerator;
import org.geysermc.mappings.generator.mcpl.BuiltinSoundGenerator;
import org.geysermc.mappings.generator.mcpl.CustomStatisticGenerator;
import org.geysermc.mappings.generator.mcpl.LevelEventTypeGenerator;
import org.geysermc.mappings.names.Renamers;
import org.geysermc.mappings.resources.BedrockSamples;
import org.geysermc.mappings.util.MappingsUtil;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/// Holds all generators generating a stub enum to a `.java` file, whether they use {@link EnumDataGenerator} or not.
///
/// Since these type of generators are fairly common, they're kept in this separate class.
public final class EnumDataGenerators {

    private EnumDataGenerators() {}

    public static ProviderGroupBuilder mcpl(ProviderGroupBuilder builder) {
        return builder.withFactory(mcplRegistry("Block Entity Type", FileType.MCPL_BLOCK_ENTITY_TYPE, BuiltInRegistries.BLOCK_ENTITY_TYPE))
                .withFactory(BuiltinSoundGenerator::new)
                .withFactory(mcplRegistry("Command Parser", FileType.MCPL_COMMAND_PARSER, BuiltInRegistries.COMMAND_ARGUMENT_TYPE))
                .withFactory(mcplRegistry("Container Type", FileType.MCPL_CONTAINER_TYPE, BuiltInRegistries.MENU))
                .withFactory(CustomStatisticGenerator::new)
                .withFactory(mcplRegistry("Effect", FileType.MCPL_EFFECT, BuiltInRegistries.MOB_EFFECT))
                .withFactory(LevelEventTypeGenerator::new)
                .withFactory(mcplRegistry("Map Icon Type", FileType.MCPL_MAP_ICON_TYPE, BuiltInRegistries.MAP_DECORATION_TYPE))
                .withFactory(mcplRegistry("Particle Type", FileType.MCPL_PARTICLE_TYPE, BuiltInRegistries.PARTICLE_TYPE));
    }

    public static ProviderGroupBuilder enums(ProviderGroupBuilder builder) {
        return builder.withRegistryAndSamplesFactory(EnumDataGenerator.protocolBasedRegistry("Bedrock Enchantment", FileType.BEDROCK_ENCHANTMENT,
                        Registries.ENCHANTMENT, FileType.BEDROCK_ENCHANTMENTS, Renamers.ENCHANTMENTS))
                .withFactory(MapColorsGenerator::new)
                .withSamplesFactory(EnumDataGenerators.interactiveTags());
    }

    private static BiFunction<PackOutput, CompletableFuture<BedrockSamples>, EnumDataGenerator> interactiveTags() {
        // Little crazy this one
        return EnumDataGenerator.samplesFactory("Interactive Tags", FileType.INTERACTIVE_TAG,
                FileType.BEDROCK_LANG_EN_US, texts -> texts.stringPropertyNames().stream()
                        .filter(key -> key.startsWith("action.interact."))
                        .map(key -> key.replace("action.interact.", "")
                                .replace(".", "_").toUpperCase(Locale.ROOT) + "(" + MappingsUtil.wrapInQuotes(key) + ")"));
    }

    private static Function<PackOutput, EnumDataGenerator> mcplRegistry(String name, FileType<String> fileType, Registry<?> registry) {
        return EnumDataGenerator.registryFactory("MCPL/" + name, fileType, registry);
    }
}
