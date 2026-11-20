package org.geysermc.mappings.transformer;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import org.geysermc.mappings.definitions.item.BedrockMapDecoration;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.definitions.protocol.EnumConstant;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.transformer.instance.BlockClassNames;
import org.geysermc.mappings.transformer.instance.BlockNames;
import org.geysermc.mappings.transformer.instance.ItemClassNames;
import org.geysermc.mappings.transformer.instance.ItemNames;
import org.geysermc.mappings.transformer.instance.ParticleMappings;
import org.geysermc.mappings.transformer.instance.sound.SoundMappings;
import org.geysermc.mappings.util.MappingsUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Transformers {
    public static final Transformer<Identifier, Identifier> BIOMES = Transformer.strong(builder -> builder
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

            .root()
            .finish(Transformer.identity())); // All other biomes have the same name
    public static final Transformer<BlockState, String> BLOCKS = BlockNames.INSTANCE;
    public static final Transformer<Block, String> BLOCK_CLASSES = BlockClassNames.INSTANCE;
    public static final Transformer<Identifier, String> ENCHANTMENTS = Transformer.strong(builder -> builder
            .mapInput(enchantment -> ResourceKey.create(Registries.ENCHANTMENT, enchantment))

            // Sweeping Edge doesn't exist on bedrock :(
            .transform(Enchantments.BINDING_CURSE, "CurseOfBinding")
            .transform(Enchantments.VANISHING_CURSE, "CurseOfVanishing")

            .root()
            // Map all other enchantments by simply converting Java name to pascal case
            .finish(identifier -> MappingsUtil.snakeToPascalCase(identifier.getPath())));
    public static final Transformer<Item, Identifier> ITEMS = ItemNames.INSTANCE;
    public static final Transformer<Item, String> ITEM_CLASSES = ItemClassNames.INSTANCE;
    public static final Transformer.Creator<Set<EnumConstant>, Holder<MapDecorationType>, Optional<? extends BedrockMapDecoration.Serialized>> MAP_DECORATIONS = bedrockDecorations -> Transformer.weak(builder -> builder
            .<BedrockMapDecoration>flatMapOutput(decoration -> decoration.serialize(bedrockDecorations))
            .mark()

            // Simple renames
            .<String>mapOutput(BedrockMapDecoration::new)
            .transform(MapDecorationTypes.PLAYER, "MarkerWhite")
            .transform(MapDecorationTypes.FRAME, "MarkerSign")
            .transform(MapDecorationTypes.RED_MARKER, "MarkerRed")
            .transform(MapDecorationTypes.BLUE_MARKER, "MarkerBlue")
            .transform(MapDecorationTypes.TARGET_POINT, "TriangleRed")
            .transform(MapDecorationTypes.PLAYER_OFF_MAP, "SquareWhite")
            .transform(MapDecorationTypes.PLAYER_OFF_LIMITS, "SmallSquareWhite")
            .transform(MapDecorationTypes.RED_X, "XWhite")
            .transform(MapDecorationTypes.SWAMP_HUT, "WitchHut")
            .transform(MapDecorationTypes.ANCIENT_CITY, "BuriedAncientCity")
            .transform(MapDecorationTypes.MINESHAFT, "BuriedMineshaft")
            .transform(MapDecorationTypes.OCEAN_RUIN_WARM, "WarmOceanRuins")

            // Banners, all one icon but with different colours
            .reset()
            .<Integer>mapOutput(rgb -> new BedrockMapDecoration("SmallSquareWhite", ARGB.color(0xFF, rgb)))
            .transform(MapDecorationTypes.WHITE_BANNER, 0xFFFFFF)
            .transform(MapDecorationTypes.ORANGE_BANNER, 0xF9801D)
            .transform(MapDecorationTypes.MAGENTA_BANNER, 0xC74EBD)
            .transform(MapDecorationTypes.LIGHT_BLUE_BANNER, 0x3AB3DA)
            .transform(MapDecorationTypes.YELLOW_BANNER, 0xFED83D)
            .transform(MapDecorationTypes.LIME_BANNER, 0x80C71F)
            .transform(MapDecorationTypes.PINK_BANNER, 0xF38BAA)
            .transform(MapDecorationTypes.GRAY_BANNER, 0x474F52)
            .transform(MapDecorationTypes.LIGHT_GRAY_BANNER, 0x9D9D97)
            .transform(MapDecorationTypes.CYAN_BANNER, 0x169C9C)
            .transform(MapDecorationTypes.PURPLE_BANNER, 0x8932B8)
            .transform(MapDecorationTypes.BLUE_BANNER, 0x3C44AA)
            .transform(MapDecorationTypes.BROWN_BANNER, 0x835432)
            .transform(MapDecorationTypes.GREEN_BANNER, 0x5E7C16)
            .transform(MapDecorationTypes.RED_BANNER, 0xB02E26)
            .transform(MapDecorationTypes.BLACK_BANNER, 0x1D1D21)

            // A unique situation where the decoration doesn't exist on Bedrock, and we replace it with a black cross
            .reset()
            .transform(MapDecorationTypes.TARGET_X, new BedrockMapDecoration("XWhite", 0xFF000000))

            // Map the remaining by simply copying Java's name
            .catchAll(BedrockMapDecoration::new));
    public static final Transformer.Creator<List<Identifier>, ParticleType<?>, Optional<? extends ParticleMapping>> PARTICLES = ParticleMappings.INSTANCE;
    // Not mapping Java SoundEvent directly because it conflicts with Cloudburst's SoundEvent
    public static final Transformer.Creator<Set<String>, String, Optional<? extends SoundMapping>> SOUND_EVENTS = SoundMappings.INSTANCE;

    private Transformers() {}

    public static <R> Transformer<? super R, String> registryKey(Registry<R> registry) {
        return i -> Objects.requireNonNull(registry.getKey(i)).getPath();
    }

    public static <R> Transformer<String, String> registryElement(Registry<R> registry, Transformer<R, String> elementTransformer) {
        return i -> registry.get(Identifier.parse(i)).map(Holder::value).map(elementTransformer).orElse(i);
    }

    public static Transformer<String, String> replace(String target, String replacement) {
        return s -> s.replace(target, replacement);
    }

    public static Transformer<String, Optional<? extends String>> findAndReplace(String regex, String replacement) {
        return string -> {
            String replaced = string.replaceAll(regex, replacement);
            if (replaced.equals(string)) {
                return Optional.empty();
            }
            return Optional.of(replaced);
        };
    }

    public static Transformer<String, String> replace(Map<String, String> map) {
        return string -> {
            for (Map.Entry<String, String> replacement : map.entrySet()) {
                string = string.replace(replacement.getKey(), replacement.getValue());
            }
            return string;
        };
    }

    public static Transformer<String, String> replaceRegex(String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        return string -> {
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches()) {
                return matcher.replaceFirst(replacement);
            }
            return string;
        };
    }

    public static Transformer<String, String> replaceGroup(String regex, Transformer<String, String> replacement) {
        Pattern pattern = Pattern.compile(regex);
        return string -> {
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches()) {
                return string.substring(0, matcher.start(1)) + replacement.apply(matcher.group(1)) + string.substring(matcher.end(1));
            }
            return string;
        };
    }

    public static Transformer<String, String> segment(String separatorRegex, int index) {
        return string -> {
            String[] split = string.split(separatorRegex);
            if (index >= split.length) {
                return string;
            }
            return split[index];
        };
    }

    public static <T> Transformer<T, Optional<T>> filter(Predicate<T> predicate) {
        return t -> predicate.test(t) ? Optional.of(t) : Optional.empty();
    }

    public static Predicate<String> contains(String string) {
        return s -> s.contains(string);
    }

    public static Predicate<String> startsWith(String string) {
        return s -> s.startsWith(string);
    }

    public static Predicate<String> endsWith(String string) {
        return s -> s.endsWith(string);
    }
}
