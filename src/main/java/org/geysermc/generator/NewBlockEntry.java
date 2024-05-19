package org.geysermc.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PairCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record NewBlockEntry(String bedrockIdentifier, CompoundTag state) {
    static final Codec<NewBlockEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("bedrock_identifier", null).forGetter(NewBlockEntry::bedrockIdentifier),
                    CompoundTag.CODEC.optionalFieldOf("state", null).forGetter(NewBlockEntry::state)
            ).apply(instance, NewBlockEntry::new));
    static final Codec<List<NewBlockEntry>> LIST_CODEC = Codec.list(CODEC);
    static final Codec<List<Pair<BlockState, NewBlockEntry>>> GENERATOR_CODEC = Codec.list(RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockState.CODEC.fieldOf("java_state").forGetter(Pair::key),
                    CODEC.fieldOf("bedrock_state").forGetter(Pair::value)
            ).apply(instance, Pair::of)
    ));
}
