package org.geysermc.mappings;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.mappings.definitions.biome.BedrockBiome;
import org.geysermc.mappings.definitions.block.BlockEntry;
import org.geysermc.mappings.definitions.block.BlockMappings;
import org.geysermc.mappings.definitions.block.BlockPalette;
import org.geysermc.mappings.definitions.shape.BlockShapeMappings;
import org.geysermc.mappings.definitions.component.ItemDataComponents;
import org.geysermc.mappings.definitions.component.TypedResolvableDataComponent;
import org.geysermc.mappings.definitions.interactions.BlockInteractionData;
import org.geysermc.mappings.definitions.item.ItemComponents;
import org.geysermc.mappings.definitions.item.ItemEntry;
import org.geysermc.mappings.definitions.item.RuntimeItemStates;
import org.geysermc.mappings.definitions.mcpl.NetworkCodec;
import org.geysermc.mappings.definitions.mcpl.NetworkTags;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.definitions.util.UtilMappings;
import org.geysermc.mappings.util.MappingsCodecs;

import java.util.List;
import java.util.Map;

/// A {@link org.geysermc.mappings.FileType} stores a path to a file (relative to some "root" folder or system),
/// its {@link Codec}, and the {@link Type} it is serialised as (either {@link Type#JSON}, {@link Type#NBT}, or {@link Type#TEXT}).
/// {@link FileType}s of type {@link Type#TEXT} are generally using a {@link String} as {@link T}.
///
/// Each file touched by the generator, be it through reading, writing, or both, must have an accompanying {@link FileType}, and must be interacted through using
/// {@link FileType}s in combination with a {@link FileSystemAccess}. There may only ever be one {@link FileType} for a given path.
///
/// All {@link FileType}s must be created as a public constant in this record. The record has static helper methods for creating new instances.
///
/// You may use {@link MappingsCodecs#JSON_ELEMENT} or {@link CompoundTag#CODEC} to read and write to {@link JsonElement}s or {@link CompoundTag}s directly,
/// however, this is discouraged and should only be used during the developing of a new generator.
///
/// @param path the path to the file (relative to the "root" folder it is present in)
/// @param codec the codec of the file
/// @param type the type this file is serialised as
/// @param managed whether this file is "managed" (written to) by the generator
/// @param <T> the type of this file when loaded at runtime
public record FileType<T>(String path, Codec<T> codec, Type type, boolean managed) {
    private static boolean bootstrapped = false;
    private static final List<FileType<?>> types = new ObjectArrayList<>();

    // These are alphabetically sorted within their groups, please keep it that way!!

    // MCPL
    public static final FileType<String> MCPL_BLOCK_EVENT = javaClass("ClientboundBlockEventPacket").parented("mcpl");
    public static final FileType<String> MCPL_BUILTIN_SOUND = javaClass("BuiltinSound").parented("mcpl");
    public static final FileType<String> MCPL_CUSTOM_STATISTIC = javaClass("CustomStatistic").parented("mcpl");
    public static final FileType<String> MCPL_LEVEL_EVENT_TYPE = javaClass("LevelEventType").parented("mcpl");
    public static final FileType<NetworkCodec> MCPL_NETWORK_CODEC = nbtData("networkCodec", NetworkCodec.CODEC).parented("mcpl");
    public static final FileType<NetworkTags> MCPL_NETWORK_TAGS = nbtData("networkTags", NetworkTags.CODEC).parented("mcpl");

    // Javaclass
    public static final FileType<String> BLOCKS = javaClass("Blocks");
    public static final FileType<String> BLOCK_STATE_PROPERTIES = javaClass("Properties");
    public static final FileType<String> GAME_RULES = javaClass("GameRules");
    public static final FileType<String> MAP_COLOR = javaClass("MapColor");
    public static final FileType<String> ITEMS = javaClass("Items");

    // Javaclass - tags
    public static final FileType<String> BLOCK_TAGS = tagClass("BlockTag");
    public static final FileType<String> DIALOG_TAGS = tagClass("DialogTag");
    public static final FileType<String> ITEM_TAGS = tagClass("ItemTag");
    public static final FileType<String> ENCHANTMENT_TAGS = tagClass("EnchantmentTag");

    // Debug data
    public static final FileType<Map<BlockState, BlockEntry>> BLOCK_MAPPINGS_DEBUG = jsonData("blocks_debug", BlockMappings.DEBUG_CODEC);

    // Mappings
    public static final FileType<List<Identifier>> ADDITIONAL_OFFHAND_ITEMS = jsonMappings("additional_offhand_items", Identifier.CODEC.listOf());
    public static final FileType<Map<Holder<Biome>, Integer>> BIOME_MAPPINGS = jsonMappings("biomes", Codec.unboundedMap(Biome.CODEC, Codec.INT.fieldOf("bedrock_id").codec()));
    public static final FileType<Map<BlockState, BlockEntry>> BLOCK_MAPPINGS = nbtMappings("blocks", BlockMappings.CODEC);
    public static final FileType<BlockShapeMappings> BLOCK_SHAPE_MAPPINGS = nbtMappings("block_shapes", BlockShapeMappings.CODEC);
    public static final FileType<BlockShapeMappings> COLLISION_MAPPINGS = nbtMappings("collisions", BlockShapeMappings.CODEC);
    public static final FileType<Map<Identifier, Identifier>> DECORATED_POT_PATTERNS = jsonMappings("decorated_pot_patterns", Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC));
    public static final FileType<BlockInteractionData> INTERACTION_MAPPINGS = jsonMappings("interactions", BlockInteractionData.CODEC);
    public static final FileType<Map<Identifier, CompoundTag>> ITEM_COMPONENTS = nbtMappings("item_components", ItemComponents.COMPONENTS_CODEC);
    public static final FileType<List<ItemDataComponents>> ITEM_DATA_COMPONENTS = jsonMappings("item_data_components", ItemDataComponents.CODEC.listOf());
    public static final FileType<Map<Item, ItemEntry>> ITEM_MAPPINGS = jsonMappings("items", Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), ItemEntry.CODEC));
    public static final FileType<Map<String, ParticleMapping>> PARTICLE_MAPPINGS = jsonMappings("particles", Codec.unboundedMap(Codec.STRING, ParticleMapping.CODEC));
    public static final FileType<String> README = text("mappings/README.md", Codec.STRING, true); // An edge case
    public static final FileType<List<List<TypedResolvableDataComponent>>> RESOLVABLE_ITEM_DATA_COMPONENTS = jsonMappings("resolvable_item_data_components", TypedResolvableDataComponent.CODEC.listOf().listOf().fieldOf("value").codec());
    public static final FileType<Map<SoundEvent, SoundMapping>> SOUND_MAPPINGS = jsonMappings("sounds", Codec.unboundedMap(MappingsCodecs.TRIMMED_SOUND_EVENT_CODEC, SoundMapping.CODEC));
    public static final FileType<UtilMappings> UTIL_MAPPINGS = jsonMappings("util", UtilMappings.CODEC);

    // bedrock-data files
    public static final FileType<BlockPalette> BLOCK_PALETTE = bedrockDataNbt("block_palette", BlockPalette.CODEC);
    public static final FileType<Map<Identifier, CompoundTag>> ITEM_COMPONENTS_PALETTE = bedrockDataNbt("item_components", ItemComponents.COMPONENTS_CODEC);
    public static final FileType<RuntimeItemStates> RUNTIME_ITEM_STATES = bedrockDataJson("runtime_item_states", RuntimeItemStates.CODEC);

    // bedrock-samples files
    public static final FileType<List<BedrockBiome>> BEDROCK_BIOMES = bedrockSamplesMetadata("mojang-biomes", BedrockBiome.CODEC.listOf().fieldOf("data_items").codec());

    // Generator
    public static final FileType<MappingsOutput.FileHashes> FILE_HASHES = jsonData("file_hashes", MappingsOutput.FileHashes.CODEC);

    public FileType {
        if (bootstrapped) {
            throw new IllegalStateException("FileType must only be instantiated in FileType");
        } else if (types.stream().anyMatch(other -> other.path.equals(path))) {
            throw new IllegalStateException("Duplicate FileType registered for path " + path);
        }
        types.add(this);
    }

    private FileType<T> parented(String parent) {
        return new FileType<>(parent + "/" + path, codec, type, managed);
    }

    private static FileType<String> tagClass(String name) {
        return javaClass("tag/" + name);
    }

    private static FileType<String> javaClass(String name) {
        return text("javaclass/" + name + ".java", Codec.STRING, true);
    }

    private static <T> FileType<T> jsonData(String name, Codec<T> codec) {
        return json("data/" + name, codec, true);
    }

    private static <T> FileType<T> nbtData(String name, Codec<T> codec) {
        return nbt("data/" + name, codec, true);
    }

    private static <T> FileType<T> jsonMappings(String name, Codec<T> codec) {
        return json("mappings/" + name, codec, true);
    }

    private static <T> FileType<T> nbtMappings(String name, Codec<T> codec) {
        return nbt("mappings/" + name, codec, true);
    }

    private static <T> FileType<T> bedrockDataJson(String name, Codec<T> codec) {
        return json(name, codec, false);
    }

    private static <T> FileType<T> bedrockDataNbt(String name, Codec<T> codec) {
        return nbt(name, codec, false);
    }

    private static <T> FileType<T> bedrockSamplesMetadata(String name, Codec<T> codec) {
        return json("metadata/vanilladata_modules/" + name, codec, false);
    }

    private static <T> FileType<T> json(String name, Codec<T> codec, boolean managed) {
        return new FileType<>(name + ".json", codec, Type.JSON, managed);
    }

    private static <T> FileType<T> nbt(String name, Codec<T> codec, boolean managed) {
        return new FileType<>(name + ".nbt", codec, Type.NBT, managed);
    }

    private static <T> FileType<T> text(String name, Codec<T> codec, boolean managed) {
        return new FileType<>(name, codec, Type.TEXT, managed);
    }

    public static List<String> getManagedPaths() {
        return types.stream().filter(FileType::managed).map(FileType::path).toList();
    }

    static {
        bootstrapped = true;
    }

    public enum Type {
        JSON,
        NBT,
        TEXT
    }
}
