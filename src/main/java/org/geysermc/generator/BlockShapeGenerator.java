package org.geysermc.generator;

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

public class BlockShapeGenerator {
    private record BlockShapeMapping(IntList indices, List<List<List<Double>>> shapes) {
        private static final Codec<BlockShapeMapping> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT_STREAM.fieldOf("indices").xmap(stream -> (IntList) new IntArrayList(stream.toArray()), IntCollection::intStream).forGetter(BlockShapeMapping::indices),
                        Codec.list(Codec.list(Codec.list(Codec.DOUBLE))).fieldOf("shapes").forGetter(BlockShapeMapping::shapes)
                ).apply(instance, BlockShapeMapping::new));
    }

    // See CollisionGenerator, it's the same thing
    public static final List<List<List<Double>>> SHAPE_LIST = Lists.newArrayList();

    public static void generate() {
        // Util.initialize() should already be called.
        final Path mappings = Path.of("mappings");
        if (!Files.exists(mappings)) {
            System.out.println("Cannot create block shapes! Did you clone submodules?");
            return;
        }

        IntList indices = new IntArrayList(Block.BLOCK_STATE_REGISTRY.size());
        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            List<List<Double>> shapeBoxes = Lists.newArrayList();
            try {
                state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().forEach(item -> {
                    List<Double> coordinateList = Lists.newArrayList();
                    // Convert Box class to an array of coordinates
                    // They need to be converted from min/max coordinates to centres and sizes
                    coordinateList.add(item.minX + ((item.maxX - item.minX) / 2));
                    coordinateList.add(item.minY + ((item.maxY - item.minY) / 2));
                    coordinateList.add(item.minZ + ((item.maxZ - item.minZ) / 2));

                    coordinateList.add(item.maxX - item.minX);
                    coordinateList.add(item.maxY - item.minY);
                    coordinateList.add(item.maxZ - item.minZ);

                    shapeBoxes.add(coordinateList);
                });
            } catch (Exception e) {
                System.out.println("Failed to get block shape for " + state);
                e.printStackTrace();
            }
            if (!SHAPE_LIST.contains(shapeBoxes)) {
                SHAPE_LIST.add(shapeBoxes);
            }
            indices.add(SHAPE_LIST.lastIndexOf(shapeBoxes));
        }

        final var blockShapeMappings = new BlockShapeMapping(indices, SHAPE_LIST);
        final DataResult<Tag> result = BlockShapeMapping.CODEC.encodeStart(NbtOps.INSTANCE, blockShapeMappings);
        result.ifSuccess(tag -> {
            try {
                NbtIo.writeCompressed((CompoundTag) tag, mappings.resolve("block_shapes.nbt"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            System.out.println("Finished writing block shape file!");
        }).ifError(error -> {
            System.out.println("Failed to encode to NBT!");
            System.out.println(error.message());
        });
    }
}