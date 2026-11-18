package org.geysermc.mappings.definitions.sound;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.mappings.util.MappingsCodecs;

import java.util.function.Function;

public sealed interface SoundData {
    Codec<SoundData> FULL_CODEC = Type.CODEC.dispatch(SoundData::type, Type::codec);
    Codec<SoundData> CODEC = Codec.either(Codec.INT, FULL_CODEC)
            .xmap(either -> either.map(SoundData::constant, Function.identity()),
                    data -> data instanceof Constant(int i) ? Either.left(i) : Either.right(data));

    static SoundData constant(int data) {
        return new Constant(data);
    }

    static SoundData block(BlockState state) {
        return new Block(state);
    }

    static SoundData note(int base) {
        return new Note(base);
    }

    Type type();

    enum Type implements StringRepresentable {
        CONSTANT("constant", Constant.MAP_CODEC),
        BLOCK("block", Block.MAP_CODEC),
        NOTE("note", Note.MAP_CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends SoundData> codec;

        Type(String name, MapCodec<? extends SoundData> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends SoundData> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    record Constant(int data) implements SoundData {
        private static final MapCodec<Constant> MAP_CODEC = Codec.INT.fieldOf("value").xmap(Constant::new, Constant::data);

        @Override
        public Type type() {
            return Type.CONSTANT;
        }
    }

    record Block(BlockState state) implements SoundData {
        private static final MapCodec<Block> MAP_CODEC = MappingsCodecs.BLOCK_STATE_STRING_CODEC.fieldOf("state").xmap(Block::new, Block::state);

        @Override
        public Type type() {
            return Type.BLOCK;
        }
    }

    record Note(int base) implements SoundData {
        private static final MapCodec<Note> MAP_CODEC = Codec.INT.fieldOf("base").xmap(Note::new, Note::base);

        @Override
        public Type type() {
            return Type.NOTE;
        }
    }
}
