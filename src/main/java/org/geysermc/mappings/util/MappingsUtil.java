package org.geysermc.mappings.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.logging.log4j.core.pattern.AnsiEscape;
import org.geysermc.mappings.MappingsGenerators;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/// Holds utility methods used across the project
public final class MappingsUtil {
    public static final CompoundTag EMPTY_TAG = new CompoundTag();

    /// @return a stream of all {@link Field}s in the given {@link Class} with the `public static final` modifiers
    public static Stream<Field> listPublicConstants(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()));
    }

    /// Delegates to {@link MappingsUtil#getConstant(Class, Field, FieldGetter)}, uses {@link Field#get(Object)} as {@link FieldGetter}.
    ///
    /// @see MappingsUtil#getConstant(Class, Field, FieldGetter)
    public static <T> T getConstant(Class<?> clazz, Field field) {
        return getConstant(clazz, field, Field::get);
    }

    /// Attempts to get a constant {@link Field} in the given {@link Class} safely, by using the given {@link FieldGetter}
    ///
    /// The {@link FieldGetter} should be a method in {@link Field}, such as {@link Field#getInt(Object)} or {@link Field#getDouble(Object)}. If the field
    /// holds any object that is not a primitive, use {@link MappingsUtil#getConstant(Class, Field)}.
    ///
    /// The {@link FieldGetter} will always receive `null` as `instance`, and is executed through {@link MappingsUtil#runReflectionSafely(Class, ReflectionOperation)},
    /// so the same rules there apply here. The obtained value is cast unsafely to {@link T}.
    ///
    /// @return the obtained constant
    public static <T> T getConstant(Class<?> clazz, Field field, FieldGetter getter) {
        return runReflectionSafely(clazz, () -> (T) getter.get(field, null));
    }

    /// Runs a {@link ReflectionOperation} "safely". When an {@link IllegalAccessException} is thrown, this is wrapped into a {@link RuntimeException}.
    public static <T> T runReflectionSafely(Class<?> clazz, ReflectionOperation<T> operation) {
        try {
            return operation.run();
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Failed to run reflection operation for class " + clazz.getSimpleName(), exception);
        }
    }

    /// Attempts to find a `public static final` {@link Field} in the given {@link Class}, that holds the given {@link Object}. The first
    /// field matching is found.
    ///
    /// The {@link Object} is compared using a `==` operator, *not* using {@link Object#equals(Object)}. If no match is found, an empty
    /// {@link Optional} is returned.
    public static Optional<String> findConstantNameInClass(Class<?> clazz, Object object) {
        return listPublicConstants(clazz)
                .filter(field -> getConstant(clazz, field) == object)
                .map(Field::getName)
                .findFirst();
    }

    /// Wraps the given string in quotes (")
    public static String wrapInQuotes(String string) {
        return "\"" + string + "\"";
    }

    /// Formats the string with the given {@link AnsiEscape} styles. Returns a string with the ANSI style codes first, then the given string, and then
    /// {@link AnsiEscape#getDefaultStyle()}.
    public static String formatString(String string, AnsiEscape... styles) {
        return AnsiEscape.createSequence(Arrays.stream(styles).map(AnsiEscape::name).toArray(String[]::new)) + string + AnsiEscape.getDefaultStyle();
    }

    /// Attempts to parse a block state string, like `minecraft:lever[powered=true]`, into a {@link BlockState}
    public static DataResult<BlockState> blockStateFromString(String string) {
        String[] split = string.split("\\[");
        String identifier = split[0];
        DataResult<BlockState> result = BuiltInRegistries.BLOCK.byNameCodec().parse(JavaOps.INSTANCE, identifier).map(Block::defaultBlockState);
        if (split.length == 1) {
            return result;
        }

        String[] properties = split[1].split(",");
        properties[properties.length - 1] = properties[properties.length - 1].replaceFirst("]$", "");
        for (String property : properties) {
            String[] propertySplit = property.split("=");
            if (propertySplit.length != 2) {
                return DataResult.error(() -> "Unable to parse property: " + property);
            }
            result = result.flatMap(state -> {
                Property<?> parsed = state.getBlock().getStateDefinition().getProperty(propertySplit[0]);
                if (parsed == null) {
                    return DataResult.error(() -> "Unknown property " + propertySplit[0] + " for block " + identifier);
                }
                return parsed.parseValue(JavaOps.INSTANCE, state, propertySplit[1]);
            });
        }

        return result;
    }

    /// Turns a block state into a string, for example `minecraft:redstone_torch[powered=true]`
    public static String blockStateToString(BlockState state) {
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (state.isSingletonState()) {
            return identifier.toString();
        }
        return identifier + "[" + blockStatePropertiesToString(state) + "]";
    }

    private static ModContainer getModContainer() {
        return FabricLoader.getInstance().getModContainer(MappingsGenerators.MOD_ID).orElseThrow();
    }

    /// Gets the `geyser:bedrock_version` field from the FMJ
    public static String getBedrockVersion() {
        return getModContainer().getMetadata().getCustomValue("geyser:bedrock_version").getAsString();
    }

    /// Gets the `geyser:human_bedrock_version` field from the FMJ
    public static String getHumanBedrockVersion() {
        return getModContainer().getMetadata().getCustomValue("geyser:human_bedrock_version").getAsString();
    }

    /// Gets the `geyser:bedrock_data_sha` field from the FMJ
    public static String getBedrockDataSha() {
        return getModContainer().getMetadata().getCustomValue("geyser:bedrock_data_sha").getAsString();
    }

    // thank you rainbow
    private static String blockStatePropertiesToString(BlockState state) {
        return PropertyValueList.of(state.getProperties().stream().map(property -> property.value(state)).toArray(Property.Value[]::new)).getKey();
    }

    @FunctionalInterface
    public interface ReflectionOperation<T> {

        T run() throws IllegalAccessException;
    }

    @FunctionalInterface
    public interface FieldGetter {

        Object get(Field field, @Nullable Object instance) throws IllegalAccessException;
    }

    private MappingsUtil() {}
}
