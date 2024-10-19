package org.geysermc.generator.state;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.cloudburstmc.nbt.NbtMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class BlockMapper {
    public static final List<BlockMapper> ALL_MAPPERS = new ArrayList<>();
    private Predicate<BlockState> conditions = null;
    private final List<BiConsumer<BlockState, CompoundTag>> blockStateMappers = new ArrayList<>();

    public BlockMapper() {
        ALL_MAPPERS.add(this);
    }

    public BlockMapper requires(Predicate<BlockState> predicate) {
        if (this.conditions == null) {
            this.conditions = predicate;
        } else {
            this.conditions = this.conditions.or(predicate);
        }
        return this;
    }

    public static BlockMapper register(Block... blocks) {
        return new BlockMapper().block(blocks);
    }

    public BlockMapper block(Block... blocks) {
        return requires(state -> {
            for (Block block : blocks) {
                if (state.is(block)) {
                    return true;
                }
            }
            return false;
        });
    }

    @SafeVarargs
    public static BlockMapper register(Class<? extends Block>... clazz) {
        return new BlockMapper().instanceOf(clazz);
    }

    @SafeVarargs
    public final BlockMapper instanceOf(Class<? extends Block>... clazz) {
        return requires(state -> {
            Block block = state.getBlock();
            for (Class<? extends Block> aClass : clazz) {
                if (aClass.isInstance(block)) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Values are the same, name is different.
     */
    public BlockMapper map(Property<?> property, String bedrockName) {
        this.blockStateMappers.add((state, tag) -> {
            Object value = state.getValue(property);
            addToTag(tag, bedrockName, value);
        });
        return this;
    }

    /**
     * Values are the same, name is different.
     */
    public BlockMapper map(EnumProperty<Direction> property, String bedrockName) {
        this.blockStateMappers.add((state, tag) -> {
            Direction value = state.getValue(property);
            tag.putString(bedrockName, value.getSerializedName());
        });
        return this;
    }

    /**
     * Bedrock name is "minecraft:cardinal_direction".
     */
    public BlockMapper mapCardinalDirection(EnumProperty<Direction> property) {
        return this.map(property, "minecraft:cardinal_direction");
    }

    /**
     * Java name and Bedrock name are same, as well as possible values
     */
    public BlockMapper directMap(Property<?> property) {
        return map(property, property.getName());
    }

    public <T extends Comparable<T>> BlockMapper transform(Property<T> property, String bedrockName, Function<T, Object> function) {
        this.blockStateMappers.add((state, tag) -> {
           Object value = function.apply(state.getValue(property));
           addToTag(tag, bedrockName, value);
        });
        return this;
    }

    /**
     * Java and Bedrock names are the same.
     */
    public <T extends Comparable<T>> BlockMapper transform(Property<T> property, Function<T, Object> function) {
        return this.transform(property, property.getName(), function);
    }

    public BlockMapper transform(String bedrockName, Function<BlockState, Object> function) {
        this.blockStateMappers.add((state, tag) -> {
            Object value = function.apply(state);
            addToTag(tag, bedrockName, value);
        });
        return this;
    }

    static void addToTag(CompoundTag tag, String bedrockName, Object value) {
        switch (value) {
            case StringRepresentable sr -> tag.putString(bedrockName, sr.getSerializedName());
            case String s -> tag.putString(bedrockName, s);
            case Integer i -> tag.putInt(bedrockName, i);
            case Boolean b -> tag.putBoolean(bedrockName, b);
            case null, default -> throw new RuntimeException("Don't know how to handle " + value + " of " + value.getClass());
        }
    }

    public void apply(BlockState state, CompoundTag tag) {
        if (this.conditions.test(state)) {
            this.blockStateMappers.forEach(consumer -> consumer.accept(state, tag));
        }
    }
}
