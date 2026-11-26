package org.geysermc.mappings.generator;

import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.PlayerEquipment;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.geysermc.mappings.definitions.interactions.BlockInteractionData;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.mixin.accessor.EntityAccessor;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.util.MappingsUtil;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class InteractionsGenerator extends MappingsGenerator<BlockInteractionData> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Transformer<BlockState, BlockInteractionData.Requirement> REQUIREMENT_GETTER = Transformer.of(builder -> builder
            .mapInput(BlockState::getBlock)
            // Interactions with Redstone wire depend on the wires around it
            .transform(Blocks.REDSTONE_WIRE, BlockInteractionData.Requirement.UNKNOWN)
            // Interactions with Bells depend on the HitResult
            .transform(Blocks.BELL, BlockInteractionData.Requirement.UNKNOWN)
            // Can't interact with light blocks without holding a light item
            .transform(Blocks.LIGHT, BlockInteractionData.Requirement.UNKNOWN)
            // Interactions with campfires depends on campfire recipes
            .transform(Blocks.CAMPFIRE, BlockInteractionData.Requirement.UNKNOWN)
            .transform(Blocks.SOUL_CAMPFIRE, BlockInteractionData.Requirement.UNKNOWN)
            // Teleports and will always consume the action
            .transform(Blocks.DRAGON_EGG, BlockInteractionData.Requirement.ALWAYS)
            // Depends on the player's hunger level
            .transform(Blocks.CAKE, BlockInteractionData.Requirement.UNKNOWN)
            .finish(InteractionsGenerator::getInteractionRequirement));
    private static final BlockHitResult EMPTY_HIT_RESULT = new BlockHitResult(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO, true);

    public InteractionsGenerator(PackOutput output) {
        super(output, FileType.INTERACTION_MAPPINGS);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return readExistingFile().thenCompose(data -> {
            if (data.isPresent() && data.get().isAccurate()) {
                // Cache will stop it from saving if necessary
                //noinspection unchecked,rawtypes - Java stuff :)
                return (CompletableFuture) saveFile(cache, data.get());
            }
            Stream<BlockInteractionData.BlockStateRequirement> requirements = StreamSupport.stream(Block.BLOCK_STATE_REGISTRY.spliterator(), true)
                    .map(state -> new BlockInteractionData.BlockStateRequirement(state, REQUIREMENT_GETTER.apply(state)));
            return saveFile(cache, BlockInteractionData.of(requirements));
        });
    }

    private static BlockInteractionData.Requirement getInteractionRequirement(BlockState state) {
        return switch (state.getBlock()) {
            case FlowerPotBlock _, SignBlock _ -> BlockInteractionData.Requirement.ALWAYS; // Contains checks for item, but will always consume the action
            case RespawnAnchorBlock _ -> BlockInteractionData.Requirement.UNKNOWN; // depends on the dimension (only works in the nether)
            default -> tryCalculateInteractionRequirement(state);
        };
    }

    private static BlockInteractionData.Requirement tryCalculateInteractionRequirement(BlockState state) {
        ClientLevel mockClientLevel = setupMockLevel();
        LocalPlayer mockPlayer = setupMockPlayer(mockClientLevel);

        Abilities abilities = new Abilities();
        abilities.mayBuild = true;

        AtomicBoolean requiresAbilities = new AtomicBoolean(false);
        Mockito.when(mockPlayer.getAbilities()).then(_ -> {
            requiresAbilities.set(true);
            return abilities;
        });

        AtomicBoolean requiresItem = new AtomicBoolean(false);
        Mockito.when(mockPlayer.getItemInHand(InteractionHand.MAIN_HAND)).then(_ -> {
            requiresItem.set(true);
            return ItemStack.EMPTY;
        });

        if (state.getBlock() instanceof BaseEntityBlock baseEntityBlock) {
            Mockito.when(mockClientLevel.getBlockEntity(BlockPos.ZERO)).thenReturn(baseEntityBlock.newBlockEntity(BlockPos.ZERO, state));
        } else {
            Mockito.when(mockClientLevel.getBlockEntity(BlockPos.ZERO)).thenReturn(null);
        }
        Mockito.when(mockClientLevel.getBlockState(BlockPos.ZERO)).thenReturn(state);

        try {
            InteractionResult result = state.useWithoutItem(mockClientLevel, mockPlayer, EMPTY_HIT_RESULT);
            if (!requiresItem.get()) {
                if (result.consumesAction() && requiresAbilities.get()) {
                    abilities.mayBuild = false;
                    InteractionResult resultWithoutMayBuild = state.useWithoutItem(mockClientLevel, mockPlayer, EMPTY_HIT_RESULT);
                    if (result != resultWithoutMayBuild) {
                        return BlockInteractionData.Requirement.REQUIRES_MAY_BUILD;
                    }
                } else if (result.consumesAction()) {
                    return BlockInteractionData.Requirement.ALWAYS;
                }
            }
        } catch (Throwable throwable) {
            // Ignore; this means the block has extended behavior we have to implement manually
            LOGGER.error("Failed to test interactions for {}!", MappingsUtil.blockStateToString(state), throwable);
        }
        return BlockInteractionData.Requirement.UNKNOWN;
    }

    private static ClientLevel setupMockLevel() {
        ClientLevel mockClientLevel = Mockito.mock(ClientLevel.class);
        RandomSource random = RandomSource.create();

        Mockito.when(mockClientLevel.isClientSide()).thenReturn(true);
        Mockito.when(mockClientLevel.getRandom()).thenReturn(random);
        Mockito.when(mockClientLevel.getBlockState(ArgumentMatchers.any())).thenReturn(Blocks.AIR.defaultBlockState());
        Mockito.when(mockClientLevel.enabledFeatures()).thenReturn(FeatureFlags.DEFAULT_FLAGS);
        return mockClientLevel;
    }

    private static LocalPlayer setupMockPlayer(ClientLevel mockLevel) {
        LocalPlayer mockPlayer = Mockito.mock(LocalPlayer.class);

        // This works, surprisingly
        ((EntityAccessor) mockPlayer).setPositionDirectly(Vec3.ZERO);

        Mockito.when(mockPlayer.level()).thenReturn(mockLevel);
        Mockito.when(mockPlayer.getInventory()).thenReturn(new Inventory(mockPlayer, new PlayerEquipment(mockPlayer)));
        Mockito.when(mockPlayer.getDirection()).thenReturn(Direction.UP); // Used by fence_gates
        Mockito.when(mockPlayer.getItemInHand(InteractionHand.OFF_HAND)).thenReturn(ItemStack.EMPTY);
        return mockPlayer;
    }

    @Override
    public String getName() {
        return "Interactions Generator";
    }
}
