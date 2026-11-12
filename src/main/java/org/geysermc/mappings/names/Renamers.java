package org.geysermc.mappings.names;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import org.geysermc.mappings.definitions.item.BedrockMapDecoration;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.names.renamers.BlockClassNames;
import org.geysermc.mappings.names.renamers.BlockNames;
import org.geysermc.mappings.names.renamers.ItemClassNames;
import org.geysermc.mappings.names.renamers.ItemNames;
import org.geysermc.mappings.names.renamers.ParticleMappings;
import org.geysermc.mappings.util.MappingsUtil;

import java.util.Optional;
import java.util.function.Function;

/// Holds {@link InstanceRenamer}s and {@link TypeRenamer}s for the various game types.
///
/// If an {@link InstanceRenamer} or {@link TypeRenamer} requires a significant size of code, it should deserve its own class in the
/// {@link org.geysermc.mappings.names.renamers} package, with a reference to it here.
public final class Renamers {
    public static final TypeRenamer<Identifier, Identifier> BIOMES = TypeRenamer.of(Function.identity(), builder -> builder
            .<ResourceKey<Biome>>mapType(ResourceKey::identifier)
            .mapResult(Identifier::withDefaultNamespace)
            // Different names:
            .rename(Biomes.BADLANDS, "mesa")
            .rename(Biomes.ERODED_BADLANDS, "mesa_bryce")
            .rename(Biomes.WOODED_BADLANDS, "mesa_plateau_stone")

            .rename(Biomes.NETHER_WASTES, "hell")
            .rename(Biomes.SOUL_SAND_VALLEY, "soulsand_valley")

            .rename(Biomes.OLD_GROWTH_BIRCH_FOREST, "birch_forest_mutated")
            .rename(Biomes.OLD_GROWTH_PINE_TAIGA, "mega_taiga")
            .rename(Biomes.OLD_GROWTH_SPRUCE_TAIGA, "redwood_taiga_mutated")

            .rename(Biomes.SNOWY_BEACH, "cold_beach")
            .rename(Biomes.SNOWY_PLAINS, "ice_plains")
            .rename(Biomes.SNOWY_TAIGA, "cold_taiga")

            .rename(Biomes.WINDSWEPT_FOREST, "extreme_hills_plus_trees")
            .rename(Biomes.WINDSWEPT_GRAVELLY_HILLS, "extreme_hills_mutated")
            .rename(Biomes.WINDSWEPT_HILLS, "extreme_hills")
            .rename(Biomes.WINDSWEPT_SAVANNA, "savanna_mutated")

            .rename(Biomes.DARK_FOREST, "roofed_forest")
            .rename(Biomes.SPARSE_JUNGLE, "jungle_edge")
            .rename(Biomes.ICE_SPIKES, "ice_plains_spikes")
            .rename(Biomes.MUSHROOM_FIELDS, "mushroom_island")
            .rename(Biomes.SWAMP, "swampland")
            .rename(Biomes.STONY_SHORE, "stone_beach")

            // Doesn't exist on bedrock:
            .rename(Biomes.END_BARRENS, "the_end")
            .rename(Biomes.END_HIGHLANDS, "the_end")
            .rename(Biomes.END_MIDLANDS, "the_end")
            .rename(Biomes.SMALL_END_ISLANDS, "the_end")
            .rename(Biomes.THE_VOID, "river")); // Not related to the end. river has similar colours.
    public static final InstanceRenamer<Block, BlockState, String> BLOCKS = BlockNames.INSTANCE;
    public static final TypeRenamer<Block, String> BLOCK_CLASSES = BlockClassNames.INSTANCE;
    public static final InstanceRenamer<Item, Identifier, Identifier> ITEMS = ItemNames.INSTANCE;
    public static final TypeRenamer<Item, String> ITEM_CLASSES = ItemClassNames.INSTANCE;
    public static final TypeRenamer<ParticleType<?>, Optional<ParticleMapping>> PARTICLES = ParticleMappings.PARTICLES;
    public static final TypeRenamer<Identifier, String> ENCHANTMENTS = TypeRenamer.of(identifier -> MappingsUtil.snakeToPascalCase(identifier.getPath()), builder -> builder
            .<ResourceKey<Enchantment>>mapType(ResourceKey::identifier)
            // Sweeping Edge doesn't exist on bedrock :(
            .rename(Enchantments.BINDING_CURSE, "CurseOfBinding")
            .rename(Enchantments.VANISHING_CURSE, "CurseOfVanishing"));
    public static final TypeRenamer<Holder<MapDecorationType>, BedrockMapDecoration> MAP_DECORATIONS = TypeRenamer.of(BedrockMapDecoration::new, builder -> builder
            // Simple renames
            .<String>mapResult(BedrockMapDecoration::new)
            .rename(MapDecorationTypes.PLAYER, "MarkerWhite")
            .rename(MapDecorationTypes.FRAME, "MarkerSign")
            .rename(MapDecorationTypes.RED_MARKER, "MarkerRed")
            .rename(MapDecorationTypes.BLUE_MARKER, "MarkerBlue")
            .rename(MapDecorationTypes.TARGET_X, "XWhite")
            .rename(MapDecorationTypes.TARGET_POINT, "TriangleRed")
            .rename(MapDecorationTypes.PLAYER_OFF_MAP, "SquareWhite")
            .rename(MapDecorationTypes.PLAYER_OFF_LIMITS, "SmallSquareWhite")
            .rename(MapDecorationTypes.RED_X, "XWhite")
            .rename(MapDecorationTypes.SWAMP_HUT, "WitchHut")
            .rename(MapDecorationTypes.ANCIENT_CITY, "BuriedAncientCity")
            .rename(MapDecorationTypes.MINESHAFT, "BuriedMineshaft")
            .rename(MapDecorationTypes.OCEAN_RUIN_WARM, "WarmOceanRuins")

            // Banners, all one icon but with different colours
            .<Integer>mapResult(rgb -> new BedrockMapDecoration("SmallSquareWhite", ARGB.color(0xFF, rgb)))
            .rename(MapDecorationTypes.WHITE_BANNER, 0xFFFFFF)
            .rename(MapDecorationTypes.ORANGE_BANNER, 0xF9801D)
            .rename(MapDecorationTypes.MAGENTA_BANNER, 0xC74EBD)
            .rename(MapDecorationTypes.LIGHT_BLUE_BANNER, 0x3AB3DA)
            .rename(MapDecorationTypes.YELLOW_BANNER, 0xFED83D)
            .rename(MapDecorationTypes.LIME_BANNER, 0x80C71F)
            .rename(MapDecorationTypes.PINK_BANNER, 0xF38BAA)
            .rename(MapDecorationTypes.GRAY_BANNER, 0x474F52)
            .rename(MapDecorationTypes.LIGHT_GRAY_BANNER, 0x9D9D97)
            .rename(MapDecorationTypes.CYAN_BANNER, 0x169C9C)
            .rename(MapDecorationTypes.PURPLE_BANNER, 0x8932B8)
            .rename(MapDecorationTypes.BLUE_BANNER, 0x3C44AA)
            .rename(MapDecorationTypes.BROWN_BANNER, 0x835432)
            .rename(MapDecorationTypes.GREEN_BANNER, 0x5E7C16)
            .rename(MapDecorationTypes.RED_BANNER, 0xB02E26)
            .rename(MapDecorationTypes.BLACK_BANNER, 0x1D1D21));

    private Renamers() {}
}
