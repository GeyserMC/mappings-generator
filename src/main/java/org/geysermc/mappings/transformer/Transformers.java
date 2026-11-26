package org.geysermc.mappings.transformer;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.geysermc.mappings.definitions.item.BedrockMapDecoration;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.definitions.protocol.EnumConstant;
import org.geysermc.mappings.transformer.instance.BiomeMappings;
import org.geysermc.mappings.transformer.instance.BlockClassNames;
import org.geysermc.mappings.transformer.instance.BlockNames;
import org.geysermc.mappings.transformer.instance.EnchantmentMappings;
import org.geysermc.mappings.transformer.instance.ItemClassNames;
import org.geysermc.mappings.transformer.instance.ItemNames;
import org.geysermc.mappings.transformer.instance.MapDecorationMappings;
import org.geysermc.mappings.transformer.instance.ParticleMappings;
import org.geysermc.mappings.transformer.instance.SoundMappings;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Transformers {
    public static final Transformer<Identifier, Identifier> BIOMES = BiomeMappings.INSTANCE;
    public static final Transformer<BlockState, String> BLOCKS = BlockNames.INSTANCE;
    public static final Transformer<Block, String> BLOCK_CLASSES = BlockClassNames.INSTANCE;
    public static final Transformer<Identifier, String> ENCHANTMENTS = EnchantmentMappings.INSTANCE;
    public static final Transformer<Item, Identifier> ITEMS = ItemNames.INSTANCE;
    public static final Transformer<Item, String> ITEM_CLASSES = ItemClassNames.INSTANCE;
    public static final Transformer.MaybeCreator<Set<EnumConstant>, Holder<MapDecorationType>, BedrockMapDecoration.Serialized> MAP_DECORATIONS = MapDecorationMappings.INSTANCE;
    public static final Transformer.MaybeCreator<List<Identifier>, ParticleType<?>, ParticleMapping> PARTICLES = ParticleMappings.INSTANCE;
    public static final Transformer.MaybeCreator<Set<String>, String, String> SOUND_EVENTS = SoundMappings.INSTANCE;

    private Transformers() {}

    public static <R> Transformer.Maybe<? super R, String> registryId(Registry<R> registry) {
        return i -> Optional.ofNullable(registry.getKey(i)).map(Identifier::getPath);
    }

    public static <R> Transformer.Maybe<? super R, Identifier> registryKey(Registry<R> registry) {
        return i -> Optional.ofNullable(registry.getKey(i));
    }

    public static <R> Transformer.Maybe<String, R> registryElement(Registry<R> registry) {
        return i -> registry.get(Identifier.parse(i)).map(Holder::value);
    }

    public static Transformer<String, String> replace(String target, String replacement) {
        return s -> s.replace(target, replacement);
    }

    public static Transformer.Maybe<String, String> findAndReplace(String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        return string -> {
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                return Optional.of(matcher.replaceAll(replacement));
            }
            return Optional.empty();
        };
    }

    public static Transformer.Maybe<String, String> findAndReplace(String regex, Transformer<MatchResult, String> replacement) {
        Pattern pattern = Pattern.compile(regex);
        return string -> {
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                return Optional.of(matcher.replaceAll(replacement));
            }
            return Optional.empty();
        };
    }

    public static Transformer.Maybe<String, String> findAndReplaceGroup(String regex, Transformer.Maybe<String, String> replacement) {
        Pattern pattern = Pattern.compile(regex);
        return string -> {
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches() && matcher.group(1) != null) {
                return replacement.apply(matcher.group(1))
                        .map(replaced -> string.substring(0, matcher.start(1)) + replaced + string.substring(matcher.end(1)));
            }
            return Optional.empty();
        };
    }

    public static Transformer.Maybe<String, String> findAndReplaceGroups(String regex, Transformer.Maybe<String, String> firstReplacement,
                                                                         Transformer.MaybeCreator<String, String, String> secondReplacement) {
        Pattern pattern = Pattern.compile(regex);
        return string -> {
            Matcher matcher = pattern.matcher(string);
            if (matcher.matches()) {
                String group1 = matcher.group(1);
                String group2 = matcher.group(2);
                if (group1 == null || group2 == null) {
                    return Optional.empty();
                }
                return firstReplacement.apply(group1).flatMap(firstReplaced -> secondReplacement.create(firstReplaced).apply(group2)
                        .map(secondReplaced
                                -> string.substring(0, matcher.start(1)) + firstReplaced
                                + string.substring(matcher.end(1), matcher.start(2)) + secondReplaced + string.substring(matcher.end(2))));
            }
            return Optional.empty();
        };
    }

    public static Transformer.Maybe<String, String> swap(String target, String replacement) {
        return string -> {
            String replaced = string.replace(target, replacement);
            if (replaced.equals(string)) {
                replaced = string.replace(replacement, target);
                if (replaced.equals(string)) {
                    return Optional.empty();
                }
            }
            return Optional.of(replaced);
        };
    }

    public static <T> Transformer.Maybe<T, T> filter(Predicate<T> predicate) {
        return t -> predicate.test(t) ? Optional.of(t) : Optional.empty();
    }

    public static Predicate<String> contains(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return s -> pattern.matcher(s).find();
    }
}
