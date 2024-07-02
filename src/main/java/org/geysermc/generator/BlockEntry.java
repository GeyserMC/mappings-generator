package org.geysermc.generator;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.math.BigDecimal;
import java.util.List;

public record BlockEntry(String bedrockIdentifier, CompoundTag state) {
    static final Codec<BlockEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("bedrock_identifier", null).forGetter(BlockEntry::bedrockIdentifier),
                    CompoundTag.CODEC.optionalFieldOf("state", new CompoundTag()).forGetter(BlockEntry::state)
            ).apply(instance, BlockEntry::new));
    static final Codec<List<BlockEntry>> LIST_CODEC = Codec.list(CODEC);
    static final Codec<List<Pair<BlockState, BlockEntry>>> GENERATOR_CODEC = Codec.list(RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("java_state").forGetter(Pair::key),
                    CODEC.fieldOf("bedrock_state").forGetter(Pair::value)
            ).apply(instance, Pair::of)
    ));

    static final JsonOps JSON_OPS_WITH_BYTE_BOOLEAN = new JsonOps(false) {
        @Override
        public <U> U convertTo(DynamicOps<U> outOps, JsonElement input) {
            if (input instanceof JsonPrimitive primitive && primitive.isNumber()) {
                BigDecimal value = primitive.getAsBigDecimal();

                // Copy the convertTo code, but do not create shorts or bytes
                try {
                    long l = value.longValueExact();
                    return (long) ((int) l) == l ? outOps.createInt((int) l) : outOps.createLong(l);
                } catch (ArithmeticException var8) {
                    double d = value.doubleValue();
                    return (double) ((float) d) == d ? outOps.createFloat((float) d) : outOps.createDouble(d);
                }
            }
            return super.convertTo(outOps, input);
        }

        @Override
        public JsonElement createByte(byte value) {
            return switch (value) {
                case 0 -> new JsonPrimitive(false);
                case 1 -> new JsonPrimitive(true);
                default -> super.createByte(value);
            };
        }
    };
}
