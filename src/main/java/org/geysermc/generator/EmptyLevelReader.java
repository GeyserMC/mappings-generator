package org.geysermc.generator;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

// match mojang
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EmptyLevelReader implements LevelReader {

    public static final EmptyLevelReader INSTANCE = new EmptyLevelReader();

    private final RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @Nullable
    @Override
    public ChunkAccess getChunk(int i, int i1, ChunkStatus chunkStatus, boolean b) {
        return null;
    }

    @Override
    public boolean hasChunk(int i, int i1) {
        return false;
    }

    @Override
    public int getHeight(Heightmap.Types types, int i, int i1) {
        return 0;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return null;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int i, int i1, int i2) {
        return null;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public DimensionType dimensionType() {
        return null;
    }

    @Override
    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return null;
    }

    @Override
    public float getShade(Direction direction, boolean b) {
        return 0;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return null;
    }

    @Override
    public WorldBorder getWorldBorder() {
        return null;
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aabb) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos blockPos) {
        return EmptyBlockGetter.INSTANCE.getBlockEntity(blockPos);
    }

    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        return EmptyBlockGetter.INSTANCE.getBlockState(blockPos);
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return EmptyBlockGetter.INSTANCE.getFluidState(blockPos);
    }
}
