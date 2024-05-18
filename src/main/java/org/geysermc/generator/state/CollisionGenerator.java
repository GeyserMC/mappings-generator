package org.geysermc.generator.state;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CollisionGenerator {
    private record CollisionsMappings(IntList indices, List<List<List<Double>>> collisions) {
        private static final Codec<CollisionsMappings> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT_STREAM.fieldOf("indices").xmap(stream -> (IntList) new IntArrayList(stream.toArray()), IntCollection::intStream).forGetter(CollisionsMappings::indices),
                        Codec.list(Codec.list(Codec.list(Codec.DOUBLE))).fieldOf("collisions").forGetter(CollisionsMappings::collisions)
                ).apply(instance, CollisionsMappings::new));
    }
    // This ends up in collision.json
    // collision_index in blocks.json refers to this to prevent duplication
    // This helps to reduce file size
    public static final List<List<List<Double>>> COLLISION_LIST = Lists.newArrayList();

    public static void generate() {
        // Util.initialize() should already be called.
        final Path mappings = Path.of("mappings");
        if (!Files.exists(mappings)) {
            System.out.println("Cannot create collisions! Did you clone submodules?");
            return;
        }

        IntList indices = new IntArrayList(Block.BLOCK_STATE_REGISTRY.size());
        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            List<List<Double>> collisionBoxes = Lists.newArrayList();
            try {
                state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().forEach(item -> {
                    List<Double> coordinateList = Lists.newArrayList();
                    // Convert Box class to an array of coordinates
                    // They need to be converted from min/max coordinates to centres and sizes
                    coordinateList.add(item.minX + ((item.maxX - item.minX) / 2));
                    coordinateList.add(item.minY + ((item.maxY - item.minY) / 2));
                    coordinateList.add(item.minZ + ((item.maxZ - item.minZ) / 2));

                    coordinateList.add(item.maxX - item.minX);
                    coordinateList.add(item.maxY - item.minY);
                    coordinateList.add(item.maxZ - item.minZ);

                    collisionBoxes.add(coordinateList);
                });
            } catch (Exception e) {
                System.out.println("Failed to get collision for " + state);
                e.printStackTrace();
            }
            if (!COLLISION_LIST.contains(collisionBoxes)) {
                COLLISION_LIST.add(collisionBoxes);
            }
            indices.add(COLLISION_LIST.lastIndexOf(collisionBoxes));
        }

        final var collisionsMappings = new CollisionsMappings(indices, COLLISION_LIST);
        final DataResult<Tag> result = CollisionsMappings.CODEC.encodeStart(NbtOps.INSTANCE, collisionsMappings);
        result.ifSuccess(tag -> {
            try {
                NbtIo.writeCompressed((CompoundTag) tag, mappings.resolve("collisions.nbt"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            System.out.println("Finished writing collision file!");
        }).ifError(error -> {
            System.out.println("Failed to encode to NBT!");
            System.out.println(error.message());
        });
    }
}
