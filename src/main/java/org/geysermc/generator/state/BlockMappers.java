package org.geysermc.generator.state;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;

import java.util.function.Function;

import static org.geysermc.generator.state.BlockMapper.register;

public final class BlockMappers {
    public static void registerMappers() {
        register(AmethystClusterBlock.class).map(AmethystClusterBlock.FACING, "minecraft:block_face");
        register(RotatedPillarBlock.class).map(RotatedPillarBlock.AXIS, "pillar_axis");
        register(BeehiveBlock.class).transform(HorizontalDirectionalBlock.FACING, "direction",
                value -> switch (value) {
                    case WEST -> 1;
                    case NORTH -> 2;
                    case EAST -> 3;
                    default -> 0;
                });
        register(BigDripleafBlock.class, BigDripleafStemBlock.class)
                .map(BlockStateProperties.HORIZONTAL_FACING, "minecraft:cardinal_direction");
        register(Blocks.BIG_DRIPLEAF).transform(BlockStateProperties.TILT, "big_dripleaf_tilt",
                value -> switch (value) {
                    case NONE -> "none";
                    case UNSTABLE -> "unstable";
                    case PARTIAL -> "partial_tilt";
                    case FULL -> "full_tilt";
                });
        register(ButtonBlock.class).transform("facing_direction", state -> {
            AttachFace facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
            return switch (facing) {
                case FLOOR -> 1;
                case WALL -> switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
                    case NORTH -> 2;
                    case SOUTH -> 3;
                    case WEST -> 4;
                    case EAST -> 5;
                    default -> 0;
                };
                default -> 0;
            };
        }).map(ButtonBlock.POWERED, "button_pressed_bit");
        register(Blocks.CALIBRATED_SCULK_SENSOR).mapCardinalDirection(CalibratedSculkSensorBlock.FACING);
        register(CampfireBlock.class).mapCardinalDirection(CampfireBlock.FACING)
                .transform(CampfireBlock.LIT, "extinguished", value -> !value);
        register(CandleBlock.class).transform(CandleBlock.CANDLES, value -> value - 1)
                .directMap(CandleBlock.LIT);
        register(LayeredCauldronBlock.class)
                .transform(LayeredCauldronBlock.LEVEL, "fill_level", value ->
                        switch (value) {
                            case 1 -> 3;
                            case 2 -> 4;
                            case 3 -> 6;
                            default -> throw new RuntimeException("Unknown cauldron liquid level!");
                        });
        register(Blocks.CHISELED_BOOKSHELF)
                .transform("books_stored", state -> {
                    // bedrock stores the book occupancy list as a bitmask.
                    int mask = 0;
                    var properties = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES;
                    for (int i = 0; i < 6; i++) {
                        BooleanProperty property = properties.get(i);
                        if (state.getValue(property)) {
                            mask |= (1 << i);
                        }
                    }
                    return mask;
                })
                .transform(HorizontalDirectionalBlock.FACING, "direction", value -> switch (value) {
                    case SOUTH -> 0;
                    case WEST -> 1;
                    case NORTH -> 2;
                    case EAST -> 3;
                    default -> throw new IllegalArgumentException("Got " + value + " instead of a cardinal direction");
                });
        register(CopperBulbBlock.class).directMap(CopperBulbBlock.LIT).map(CopperBulbBlock.POWERED, "powered_bit");
        register(Blocks.CRAFTER)
                .directMap(CrafterBlock.CRAFTING)
                .directMap(BlockStateProperties.ORIENTATION)
                .map(CrafterBlock.TRIGGERED, "triggered_bit");
        register(Blocks.DECORATED_POT).transform(BlockStateProperties.HORIZONTAL_FACING, "direction",
                value -> switch (value) {
                    case NORTH -> 0;
                    case EAST -> 1;
                    case SOUTH -> 2;
                    case WEST -> 3;
                    default -> throw new IllegalArgumentException("Got " + value + " instead of a cardinal direction");
                });
        register(DoorBlock.class)
                .transform(DoorBlock.FACING, "direction", value -> switch (value) {
                    case NORTH -> 3;
                    case SOUTH -> 1;
                    case WEST -> 2;
                    default -> 0;
                })
                .transform(DoorBlock.HALF, "upper_block_bit", value -> value == DoubleBlockHalf.UPPER)
                .transform(DoorBlock.HINGE, "door_hinge_bit", value -> value == DoorHingeSide.RIGHT)
                .map(DoorBlock.OPEN, "open_bit");
        register(FenceGateBlock.class)
                .transform(FenceGateBlock.FACING, "direction",
                   value -> switch (value) {
                        case NORTH -> 2;
                        case WEST -> 1;
                        case EAST -> 3;
                        default -> 0;
                })
                .map(FenceGateBlock.IN_WALL, "in_wall_bit")
                .map(FenceGateBlock.OPEN, "open_bit");

        register(
                Blocks.BLAST_FURNACE,
                Blocks.FURNACE,
                Blocks.SMOKER,
                Blocks.ANVIL,
                Blocks.CHIPPED_ANVIL,
                Blocks.DAMAGED_ANVIL,
                Blocks.END_PORTAL_FRAME,
                Blocks.LECTERN,
                Blocks.PINK_PETALS,
                Blocks.COMPARATOR,
                Blocks.REPEATER,
                Blocks.SOUL_CAMPFIRE,
                Blocks.CHEST,
                Blocks.ENDER_CHEST,
                Blocks.STONECUTTER,
                Blocks.TRAPPED_CHEST
        ).mapCardinalDirection(HorizontalDirectionalBlock.FACING);

        register(LeavesBlock.class).map(LeavesBlock.PERSISTENT, "persistent_bit");
        register(Blocks.LIGHT).map(LightBlock.LEVEL, "block_light_level");
        register(Blocks.WATER, Blocks.LAVA).map(LiquidBlock.LEVEL, "liquid_depth");
        register(Blocks.MANGROVE_PROPAGULE)
                .directMap(MangrovePropaguleBlock.HANGING)
                .map(MangrovePropaguleBlock.STAGE, "propagule_stage");
        register(Blocks.OBSERVER).map(ObserverBlock.FACING, "minecraft:facing_direction");
        register(Blocks.TRIAL_SPAWNER, Blocks.VAULT).directMap(BlockStateProperties.OMINOUS);
        register(Blocks.PINK_PETALS)
                .transform(PinkPetalsBlock.AMOUNT, "growth", value -> {
                    // Java has flower_amount   1, 2, 3, 4
                    // Bedrock has growth       0, 1, 2, 3, 4, 5, 6, 7
                    // but apparently growth greater than 3 can only be obtained via commands: https://minecraft.fandom.com/wiki/Pink_Petals
                    return value - 1;
                })
                .mapCardinalDirection(PinkPetalsBlock.FACING);
        register(Blocks.PITCHER_CROP)
                .transform(PitcherCropBlock.AGE, "growth", value -> switch (value) {
                    case 0 -> 0;
                    case 1 -> 1;
                    case 2 -> 3;
                    case 3 -> 5;
                    default -> 7; // 4 -> 7
                })
                .transform(PitcherCropBlock.HALF, "upper_block_bit", value -> value == DoubleBlockHalf.UPPER);
        register(PressurePlateBlock.class).transform(PressurePlateBlock.POWERED, "redstone_signal", value -> value ? 15 : 0);
        register(RailBlock.class).transform(RailBlock.SHAPE, "rail_direction", value -> switch (value) {
            case NORTH_SOUTH -> 0;
            case EAST_WEST -> 1;
            case ASCENDING_EAST -> 2;
            case ASCENDING_WEST -> 3;
            case ASCENDING_NORTH -> 4;
            case ASCENDING_SOUTH -> 5;
            case SOUTH_EAST -> 6;
            case SOUTH_WEST -> 7;
            case NORTH_WEST -> 8;
            case NORTH_EAST -> 9;
        });
        register(PoweredRailBlock.class).map(PoweredRailBlock.POWERED, "rail_data_bit");
        register(Blocks.SCULK_CATALYST).directMap(SculkCatalystBlock.PULSE); // Is "bloom" as of 1.20.5
        register(SculkSensorBlock.class).transform(SculkSensorBlock.PHASE, value -> switch (value) { // calibrated_sculk_sensor and sculk_sensor
            case INACTIVE -> 0;
            case ACTIVE -> 1;
            case COOLDOWN -> 2;
        });
        register(Blocks.SCULK_SHRIEKER)
                .directMap(SculkShriekerBlock.CAN_SUMMON)
                .map(SculkShriekerBlock.SHRIEKING, "active");
        register(SlabBlock.class).transform(SlabBlock.TYPE, "minecraft:vertical_half", value -> value == SlabType.TOP ? "top" : "bottom");
        register(Blocks.SMALL_DRIPLEAF).transform(SmallDripleafBlock.FACING, "minecraft:cardinal_direction", value -> {
            // Bedrock's small drip leaves are rotated clockwise once for the same direction, so these values are shifted around
            return switch (value) {
                case SOUTH -> "east";
                case WEST -> "south";
                case NORTH -> "west";
                case EAST -> "north";
                default -> throw new RuntimeException("Could not determine small dripleaf direction!");
            };
        });
        register(StairBlock.class).transform(StairBlock.FACING, "weirdo_direction", value ->
                switch (value) {
                    case NORTH -> 3;
                    case SOUTH -> 2;
                    case WEST -> 1;
                    default -> 0;
                })
                .transform(StairBlock.HALF, "upside_down_bit", value -> value == Half.TOP);
        register(AttachedStemBlock.class).transform(AttachedStemBlock.FACING, "facing_direction",
                value -> switch (value) {
                    case NORTH -> 2;
                    case WEST -> 4;
                    case SOUTH -> 3;
                    case EAST -> 5;
                    default -> 0;
                });
        register(BrushableBlock.class)
                .map(BlockStateProperties.DUSTED, "brushed_progress")
                .transform("hanging", state -> false); // seemingly undeterminable
        register(WallTorchBlock.class).transform(WallTorchBlock.FACING, "torch_facing_direction", value -> switch (value) {
            case NORTH -> "south";
            case WEST -> "east";
            case SOUTH -> "north";
            case EAST -> "west";
            default -> "";
        });
        register(TrapDoorBlock.class)
                .transform(TrapDoorBlock.FACING, "direction", value -> switch (value) {
                    case NORTH -> 3;
                    case SOUTH -> 2;
                    case WEST -> 1;
                    default -> 0;
                })
                .transform(TrapDoorBlock.HALF, "upside_down_bit", value -> value == Half.TOP);
        register(Blocks.TRIAL_SPAWNER).transform(TrialSpawnerBlock.STATE, value -> switch (value) {
            case INACTIVE -> 0;
            case WAITING_FOR_PLAYERS -> 1;
            case ACTIVE -> 2;
            case WAITING_FOR_REWARD_EJECTION -> 3;
            case EJECTING_REWARD -> 4;
            case COOLDOWN -> 5;
        });
        register(Blocks.TWISTING_VINES).map(TwistingVinesBlock.AGE, "twisting_vines_age");
        register(Blocks.TWISTING_VINES_PLANT).transform("twisting_vines_age", state -> 0);
        register(Blocks.WEEPING_VINES).map(WeepingVinesBlock.AGE, "weeping_vines_age");
        register(Blocks.WEEPING_VINES_PLANT).transform("weeping_vines_age", state -> 0);
        register(Blocks.CAVE_VINES).map(CaveVinesBlock.AGE, "growing_plant_age");
        register(Blocks.CAVE_VINES_PLANT).transform("growing_plant_age", state -> 0);
        register(WallSkullBlock.class).transform(WallSkullBlock.FACING, "facing_direction", value -> switch (value) {
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 0;
        });

        /*
        Note on the attached_bit mapping:
         * If the hanging sign is attached, two chains come off the top of the sign and come to a point. If the hanging sign is
         * not attached, the two chains hang completely vertically.
         *
         * The most important thing is that if attached is false, the hanging sign is always facing a cardinal direction.
         *
         * Note that this mapper covers both wall_hanging and hanging signs. attached_bit does not matter for wall_hanging signs,
         * in which the BE state "hanging" is false.
         *
         * Extra information: if the hanging sign is hanging from a solid block, attached may be true or false, depending on if
         * it was placed while crouching. However, if the hanging sign is below a chain, attached is true.
         */
        register(CeilingHangingSignBlock.class)
                .map(CeilingHangingSignBlock.ATTACHED, "attached_bit")
                .transform(CeilingHangingSignBlock.ROTATION, "facing_direction", value -> {
                    // Seems like 'facing_direction' is used if 'attached' is false, in which the sign points
                    // in a cardinal direction (rather 16 different possible rotation values)
                    return switch (value) {
                        case 0 -> 3; // South rotation to south facing
                        case 4 -> 4; // West rotation to west facing
                        case 8 -> 2; // North rotation to north facing
                        case 12 -> 5; // East rotation to east facing
                        default -> 2;
                        // Any other value is a rotation that does NOT point in a cardinal direction. The mapped value theoretically
                        // shouldn't matter because if the rotation is not cardinal, the sign must have 'attached' true, in which
                        // 'ground_sign_direction' is used instead of 'facing_direction'
                    };
                })
                .transform("hanging", state -> true);
        register(WallHangingSignBlock.class)
                .transform("attached_bit", state -> true)
                // wall_hanging signs are "not hanging" on BE.
                // BE only has one identifier for both wall_hanging and hanging signs, so it uses this state to distinguish
                .transform("hanging", state -> false)
                /*
                 * Covers ground_sign_direction for wall_hanging signs. Theoretically, this state is not used for wall_hanging_signs,
                 * which face a cardinal direction (and use the "facing_direction" state), and have "hanging" false
                 */
                .transform(WallHangingSignBlock.FACING, "ground_sign_direction", value -> switch (value) {
                    case SOUTH -> 0;
                    case WEST -> 4;
                    case NORTH -> 8;
                    case EAST -> 12;
                    default -> throw new IllegalArgumentException("Got " + value + " instead of a cardinal direction");
                });

        register(WallHangingSignBlock.class, WallSignBlock.class).transform(WallSignBlock.FACING, "facing_direction", value -> switch (value) {
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 0;
        });

        Function<WallSide, Object> wallDirectionMapper = value -> {
            if (value == WallSide.LOW) {
                return "short";
            }
            return value.getSerializedName();
        };
        register(WallBlock.class)
                .transform("wall_block_type", state -> {
                    // Most walls follow the same naming pattern but not end brick walls
                    if (state.is(Blocks.END_STONE_BRICK_WALL)) {
                        return "end_brick";
                    }
                    ResourceLocation identifier = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    String path = identifier.getPath();
                    return path.replace("_wall", "");
                })
                .transform(WallBlock.NORTH_WALL, "wall_connection_type_north", wallDirectionMapper)
                .transform(WallBlock.SOUTH_WALL, "wall_connection_type_south", wallDirectionMapper)
                .transform(WallBlock.EAST_WALL, "wall_connection_type_east", wallDirectionMapper)
                .transform(WallBlock.WEST_WALL, "wall_connection_type_west", wallDirectionMapper)
                .map(WallBlock.UP, "wall_post_bit");
    }

}
