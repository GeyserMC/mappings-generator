package org.geysermc.mappings.transformer.instance;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.geysermc.mappings.transformer.Transformer;

public final class BiomeMappings {
    public static final Transformer<Identifier, Identifier> INSTANCE = Transformer.of(builder -> builder
            .<ResourceKey<Biome>>mapInput(biome -> ResourceKey.create(Registries.BIOME, biome))
            .mapOutput(Identifier::withDefaultNamespace)
            // Different names:
            .transform(Biomes.BADLANDS, "mesa")
            .transform(Biomes.ERODED_BADLANDS, "mesa_bryce")
            .transform(Biomes.WOODED_BADLANDS, "mesa_plateau_stone")

            .transform(Biomes.NETHER_WASTES, "hell")
            .transform(Biomes.SOUL_SAND_VALLEY, "soulsand_valley")

            .transform(Biomes.OLD_GROWTH_BIRCH_FOREST, "birch_forest_mutated")
            .transform(Biomes.OLD_GROWTH_PINE_TAIGA, "mega_taiga")
            .transform(Biomes.OLD_GROWTH_SPRUCE_TAIGA, "redwood_taiga_mutated")

            .transform(Biomes.SNOWY_BEACH, "cold_beach")
            .transform(Biomes.SNOWY_PLAINS, "ice_plains")
            .transform(Biomes.SNOWY_TAIGA, "cold_taiga")

            .transform(Biomes.WINDSWEPT_FOREST, "extreme_hills_plus_trees")
            .transform(Biomes.WINDSWEPT_GRAVELLY_HILLS, "extreme_hills_mutated")
            .transform(Biomes.WINDSWEPT_HILLS, "extreme_hills")
            .transform(Biomes.WINDSWEPT_SAVANNA, "savanna_mutated")

            .transform(Biomes.DARK_FOREST, "roofed_forest")
            .transform(Biomes.SPARSE_JUNGLE, "jungle_edge")
            .transform(Biomes.ICE_SPIKES, "ice_plains_spikes")
            .transform(Biomes.MUSHROOM_FIELDS, "mushroom_island")
            .transform(Biomes.SWAMP, "swampland")
            .transform(Biomes.STONY_SHORE, "stone_beach")

            // Doesn't exist on bedrock:
            .transform(Biomes.END_BARRENS, "the_end")
            .transform(Biomes.END_HIGHLANDS, "the_end")
            .transform(Biomes.END_MIDLANDS, "the_end")
            .transform(Biomes.SMALL_END_ISLANDS, "the_end")
            .transform(Biomes.THE_VOID, "river") // Not related to the end. river has similar colours.

            .finish(Transformer.identity())); // All other biomes have the same name

    private BiomeMappings() {}
}
