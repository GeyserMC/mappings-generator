package org.geysermc.mappings.generator.javaclass;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import org.geysermc.mappings.definitions.block.BlockProperties;
import org.geysermc.mappings.generator.MappingsGenerator;
import org.geysermc.mappings.FileType;
import org.geysermc.mappings.names.Renamers;
import org.geysermc.mappings.util.FieldConstructor;
import org.geysermc.mappings.util.MappingsUtil;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.geysermc.mappings.util.MappingsUtil.wrapInQuotes;

public final class BlocksGenerator extends MappingsGenerator<String> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CompletableFuture<RegistryAccess> registries;

    public BlocksGenerator(PackOutput output, CompletableFuture<RegistryAccess> registries) {
        super(output, FileType.BLOCKS);
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(_ -> {
            StringBuilder clazz = new StringBuilder();

            BuiltInRegistries.BLOCK.forEach(block -> {
                FieldConstructor constructor = new FieldConstructor("Block");
                String blockClass = Renamers.BLOCK_CLASSES.get(block);

                Identifier key = BuiltInRegistries.BLOCK.getKey(block);
                String path = key.getPath();
                constructor.declareFieldName(path).declareClassName(blockClass).addParameter(wrapInQuotes(path));

                switch (block) {
                    case AbstractBannerBlock banner -> constructor.addParameter(banner.getColor().getId());
                    case AbstractSkullBlock skull -> constructor.addParameter("SkullBlock.Type." + ((SkullBlock.Types) skull.getType()).name());
                    case BedBlock bed -> constructor.addParameter(bed.getColor().getId());
                    case FlowerPotBlock pot -> constructor.addParameter(BuiltInRegistries.BLOCK.getKey(pot.getPotted()).getPath().toUpperCase(Locale.ROOT));
                    default -> {}
                }

                constructor.finishParameters();

                // The following properties are the same for each block state

                BlockState defaultState = block.defaultBlockState();
                if (block instanceof EntityBlock entityBlock) {
                    BlockEntityType<?> type = null;
                    BlockEntity entity = entityBlock.newBlockEntity(BlockPos.ZERO, defaultState);
                    if (entity == null) {
                        // EntityBlock#newBlockEntity is only null for pistons, as they have a separate method...
                        if (defaultState.getBlock() instanceof MovingPistonBlock) {
                            type = BlockEntityTypes.PISTON;
                        } else {
                            LOGGER.error("Did not find block entity type for block {}!", key);
                        }
                    } else {
                        type = entity.getType();
                    }
                    if (type != null) {
                        constructor.addMethod("setBlockEntity", "BlockEntityType." + Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)).getPath().toUpperCase(Locale.ROOT));
                    }
                }
                if (defaultState.requiresCorrectToolForDrops()) {
                    constructor.addMethod("requiresCorrectToolForDrops");
                }
                float destroyTime = block.defaultDestroyTime();
                if (destroyTime != 0.0F) {
                    constructor.addMethod("destroyTime", destroyTime);
                }
                PushReaction pushReaction = defaultState.getPistonPushReaction();
                if (pushReaction != PushReaction.PUSH_PULL) {
                    constructor.addMethod("pushReaction", "PistonBehavior." + pushReaction);
                }

                // Add the block state properties

                Collection<Property<?>> properties = block.defaultBlockState().getProperties();
                properties.forEach(property -> {
                    Optional<String> fieldName = MappingsUtil.findConstantNameInClass(BlockStateProperties.class, property);
                    fieldName.ifPresentOrElse(propertyName -> {
                        switch (property) {
                            case EnumProperty<?> enumProperty -> {
                                if (enumProperty.getValueClass().equals(Direction.class)) {
                                    List<String> collection = BlockProperties.allDirections((EnumProperty<Direction>) enumProperty);
                                    constructor.newline().addMethod("enumState", propertyName, String.join(", ", collection));
                                } else if (BlockProperties.geyserHasEnum(enumProperty.getValueClass())) {
                                    constructor.newline().addMethod("enumState", propertyName, BlockProperties.allEnums(enumProperty));
                                } else {
                                    // Geyser's BasicEnumProperty stores the values for BlockState value switching
                                    constructor.newline().addMethod("enumState", propertyName);
                                }
                            }
                            // Geyser's IntegerProperty has to store the low and high anyway, so we'll get the rates
                            // from there.
                            case IntegerProperty _ -> constructor.newline().addMethod("intState", propertyName);
                            case BooleanProperty _ -> constructor.newline().addMethod("booleanState", propertyName);
                            default -> LOGGER.error("Don't know what to do with state property {} for block {}!", property, key);
                        }
                    }, () -> LOGGER.error("Unable to find constant name for state property {} for block {}!", property, key));
                });

                clazz.append(constructor.finish()).append("\n");
            });

            return saveFile(cache, clazz.toString());
        });
    }

    @Override
    public String getName() {
        return "Blocks Generator";
    }
}
