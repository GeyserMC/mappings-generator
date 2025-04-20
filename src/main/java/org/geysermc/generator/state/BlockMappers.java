package org.geysermc.generator.state;

import net.minecraft.core.Direction;
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
        register(Blocks.CAULDRON).addBedrockProperty("fill_level", 0);
        register(Blocks.LAVA_CAULDRON).addBedrockProperty("cauldron_liquid", "lava");
        register(Blocks.WATER_CAULDRON, Blocks.CAULDRON).addBedrockProperty("cauldron_liquid", "water");
        register(Blocks.POWDER_SNOW_CAULDRON).addBedrockProperty("cauldron_liquid", "powder_snow");
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
                // This is NOT a direct mappings to minecraft:cardinal_direction!!!
                .transform(DoorBlock.FACING, "minecraft:cardinal_direction", value -> switch (value) {
                    case NORTH -> "east";
                    case SOUTH -> "west";
                    case WEST -> "north";
                    default -> "south";
                })
                .transform(DoorBlock.HALF, "upper_block_bit", value -> value == DoubleBlockHalf.UPPER)
                .transform(DoorBlock.HINGE, "door_hinge_bit", value -> value == DoorHingeSide.RIGHT)
                .map(DoorBlock.OPEN, "open_bit");
        register(FenceGateBlock.class)
                .mapCardinalDirection(FenceGateBlock.FACING)
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
                Blocks.COMPARATOR,
                Blocks.REPEATER,
                Blocks.SOUL_CAMPFIRE,
                Blocks.CHEST,
                Blocks.ENDER_CHEST,
                Blocks.STONECUTTER,
                Blocks.TRAPPED_CHEST,
                Blocks.CARVED_PUMPKIN,
                Blocks.JACK_O_LANTERN
        ).mapCardinalDirection(HorizontalDirectionalBlock.FACING);
        register(Blocks.REPEATER)
                .transform(RepeaterBlock.DELAY, "repeater_delay", value -> value -1);
        register(Blocks.COMPARATOR)
                .map(ComparatorBlock.POWERED, "output_lit_bit")
                .transform(ComparatorBlock.MODE, "output_subtract_bit", value -> switch (value) {
                    case COMPARE -> false;
                    case SUBTRACT -> true;
                });
        register(Blocks.OBSERVER).map(ObserverBlock.POWERED, "powered_bit");
        register(Blocks.DAYLIGHT_DETECTOR).map(DaylightDetectorBlock.POWER, "redstone_signal");
        register(LeavesBlock.class)
                .map(LeavesBlock.PERSISTENT, "persistent_bit");
        register(Blocks.WATER, Blocks.LAVA).map(LiquidBlock.LEVEL, "liquid_depth");
        register(Blocks.MANGROVE_PROPAGULE)
                .directMap(MangrovePropaguleBlock.HANGING)
                .map(MangrovePropaguleBlock.AGE, "propagule_stage");
        register(Blocks.OBSERVER).map(ObserverBlock.FACING, "minecraft:facing_direction");
        register(Blocks.TRIAL_SPAWNER, Blocks.VAULT).directMap(BlockStateProperties.OMINOUS);
        register(Blocks.PINK_PETALS, Blocks.WILDFLOWERS)
                .transform(FlowerBedBlock.AMOUNT, "growth", value -> {
                    // Java has flower_amount   1, 2, 3, 4
                    // Bedrock has growth       0, 1, 2, 3, 4, 5, 6, 7
                    // but apparently growth greater than 3 can only be obtained via commands: https://minecraft.fandom.com/wiki/Pink_Petals
                    return value - 1;
                })
                .mapCardinalDirection(FlowerBedBlock.FACING);
        register(Blocks.VAULT)
                .mapCardinalDirection(VaultBlock.FACING)
                .directMap(VaultBlock.STATE);
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
        register(WeightedPressurePlateBlock.class).map(WeightedPressurePlateBlock.POWER, "redstone_signal");
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
        register(PoweredRailBlock.class, DetectorRailBlock.class)
                .map(PoweredRailBlock.POWERED, "rail_data_bit")
                .transform(PoweredRailBlock.SHAPE, "rail_direction", value -> switch (value) {
                    case NORTH_SOUTH -> 0;
                    case EAST_WEST -> 1;
                    case ASCENDING_EAST -> 2;
                    case ASCENDING_WEST -> 3;
                    case ASCENDING_NORTH -> 4;
                    case ASCENDING_SOUTH -> 5;
                    default -> throw new IllegalStateException("Unexpected value: " + value);
                });
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
                })
                .addBedrockProperty("growth", 7);
        register(StemBlock.class)
                .addBedrockProperty("facing_direction", 0)
                .map(StemBlock.AGE, "growth");
        register(BrushableBlock.class)
                .map(BlockStateProperties.DUSTED, "brushed_progress")
                .transform("hanging", state -> false); // seemingly undeterminable
        register(WallTorchBlock.class, RedstoneWallTorchBlock.class).transform(WallTorchBlock.FACING, "torch_facing_direction", value -> switch (value) {
            case NORTH -> "south";
            case WEST -> "east";
            case SOUTH -> "north";
            case EAST -> "west";
            default -> throw new IllegalStateException("Unexpected value: " + value);
        });
        register(Blocks.TORCH, Blocks.REDSTONE_TORCH, Blocks.SOUL_TORCH).addBedrockProperty("torch_facing_direction", "top");
        register(Blocks.FIRE).directMap(FireBlock.AGE);
        register(Blocks.SOUL_FIRE).addBedrockProperty("age", 0);
        register(TrapDoorBlock.class)
                .transform(TrapDoorBlock.FACING, "direction", value -> switch (value) {
                    case NORTH -> 3;
                    case SOUTH -> 2;
                    case WEST -> 1;
                    default -> 0;
                })
                .transform(TrapDoorBlock.HALF, "upside_down_bit", value -> value == Half.TOP)
                .map(BlockStateProperties.OPEN, "open_bit");
        register(Blocks.TRIAL_SPAWNER).transform(TrialSpawnerBlock.STATE, value -> switch (value) {
            case INACTIVE -> 0;
            case WAITING_FOR_PLAYERS -> 1;
            case ACTIVE -> 2;
            case WAITING_FOR_REWARD_EJECTION -> 3;
            case EJECTING_REWARD -> 4;
            case COOLDOWN -> 5;
        });
        register(Blocks.CACTUS).directMap(CactusBlock.AGE);
        register(Blocks.SUGAR_CANE).directMap(SugarCaneBlock.AGE);
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
                .map(CeilingHangingSignBlock.ROTATION, "ground_sign_direction") // even if not used, it's still present for hanging signs
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

        register(StandingSignBlock.class).map(BlockStateProperties.ROTATION_16, "ground_sign_direction");

        Function<WallSide, Object> wallDirectionMapper = value -> {
            if (value == WallSide.LOW) {
                return "short";
            }
            return value.getSerializedName();
        };
        register(WallBlock.class)
                .transform(WallBlock.NORTH, "wall_connection_type_north", wallDirectionMapper)
                .transform(WallBlock.SOUTH, "wall_connection_type_south", wallDirectionMapper)
                .transform(WallBlock.EAST, "wall_connection_type_east", wallDirectionMapper)
                .transform(WallBlock.WEST, "wall_connection_type_west", wallDirectionMapper)
                .map(WallBlock.UP, "wall_post_bit");

        register(SaplingBlock.class).additionalRequirement(state -> !(state.getBlock() instanceof MangrovePropaguleBlock))
                .transform(BlockStateProperties.STAGE, "age_bit", value -> {
                    if (value == 0) {
                        return false;
                    } else if (value == 1) {
                        return true;
                    } else {
                        throw new IllegalStateException("Unknown stage property!");
                    }
                });

        register(Blocks.PALE_HANGING_MOSS).directMap(BlockStateProperties.TIP);

        register(CreakingHeartBlock.class)
                .directMap(BlockStateProperties.NATURAL)
                .map(BlockStateProperties.AXIS, "pillar_axis")
                .directMap(CreakingHeartBlock.STATE);

        register(MossyCarpetBlock.class)
                .transform(BlockStateProperties.EAST_WALL, "pale_moss_carpet_side_east", wallDirectionMapper)
                .transform(BlockStateProperties.SOUTH_WALL, "pale_moss_carpet_side_south", wallDirectionMapper)
                .transform(BlockStateProperties.WEST_WALL, "pale_moss_carpet_side_west", wallDirectionMapper)
                .transform(BlockStateProperties.NORTH_WALL, "pale_moss_carpet_side_north", wallDirectionMapper)
                .transform(BlockStateProperties.BOTTOM, "upper_block_bit", value -> !value);

        register(LeafLitterBlock.class)
                .transform(LeafLitterBlock.AMOUNT, "growth", value -> {
                    // Java has flower_amount   1, 2, 3, 4
                    // Bedrock has growth       0, 1, 2, 3, 4, 5, 6, 7
                    // but apparently growth greater than 3 can only be obtained via commands: https://minecraft.fandom.com/wiki/Pink_Petals
                    return value - 1;
                })
                .mapCardinalDirection(LeafLitterBlock.FACING);

        // Glow liichen, sculk vein, resin clump
        register(MultifaceBlock.class)
                .transform("multi_face_direction_bits", (state -> {
                    int bitset = 0;
                    if (state.getValue(BlockStateProperties.DOWN)) {
                        bitset |= 1;
                    }
                    if (state.getValue(BlockStateProperties.UP)) {
                        bitset |= 1 << 1;
                    }
                    if (state.getValue(BlockStateProperties.SOUTH)) {
                        bitset |= 1 << 2;
                    }
                    if (state.getValue(BlockStateProperties.WEST)) {
                        bitset |= 1 << 3;
                    }
                    if (state.getValue(BlockStateProperties.NORTH)) {
                        bitset |= 1 << 4;
                    }
                    if (state.getValue(BlockStateProperties.EAST)) {
                        bitset |= 1 << 5;
                    }
                    return bitset;
                }));

        register(Blocks.DROPPER, Blocks.DISPENSER)
                .mapFacingDirection(BlockStateProperties.FACING)
                .map(BlockStateProperties.TRIGGERED, "triggered_bit");

        register(BedBlock.class)
                .transform(BedBlock.PART, "head_piece_bit", value -> switch (value) {
                    case FOOT -> false;
                    case HEAD -> true;
                })
                .map(BedBlock.OCCUPIED, "occupied_bit")
                .transform(BedBlock.FACING, "direction", direction -> switch (direction) {
                    case SOUTH -> 0;
                    case WEST -> 1;
                    case NORTH -> 2;
                    case EAST -> 3;
                    default -> throw new IllegalStateException("Unexpected value: " + direction);
                });
        register(Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.PISTON_HEAD, Blocks.END_ROD)
                .mapFacingDirection(BlockStateProperties.FACING);
        register(Blocks.SEAGRASS).addBedrockProperty("sea_grass_type", "default");
        register(Blocks.TALL_SEAGRASS).transform(TallSeagrassBlock.HALF, "sea_grass_type", value -> switch (value) {
            case UPPER -> "double_top";
            case LOWER -> "double_bot";
        });
        register(Blocks.TNT).map(TntBlock.UNSTABLE, "explode_bit");
        register(Blocks.REDSTONE_WIRE).map(BlockStateProperties.POWER, "redstone_signal");
        register(Blocks.WHEAT).map(CropBlock.AGE, "growth");
        register(Blocks.FARMLAND).map(FarmBlock.MOISTURE, "moisturized_amount");
        register(Blocks.LADDER).transform(LadderBlock.FACING, "facing_direction", direction -> switch (direction) {
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        });
        register(Blocks.LEVER)
            .map(LeverBlock.POWERED, "open_bit")
            .transform("lever_direction", state -> {
               AttachFace face = state.getValue(LeverBlock.FACE);
               Direction direction = state.getValue(LeverBlock.FACING);

               switch (face) {
                   case FLOOR -> {
                       switch (direction) {
                           case NORTH, SOUTH -> {
                               return "up_north_south";
                           }
                           case EAST, WEST -> {
                               return "up_east_west";
                           }
                       }
                   }
                   case WALL -> {
                       return direction.name().toLowerCase();
                   }
                   case CEILING -> {
                       switch (direction) {
                           case NORTH, SOUTH -> {
                               return "down_north_south";
                           }
                           case EAST, WEST -> {
                               return "down_east_west";
                           }
                       }
                   }
               }
               throw new IllegalStateException("Unexpected value: " + face);
            });
        register(Blocks.SNOW)
                .addBedrockProperty("covered_bit", false)
                .transform(SnowLayerBlock.LAYERS, "height", value -> value - 1);
        register(Blocks.NETHER_PORTAL).map(NetherPortalBlock.AXIS, "portal_axis");
        register(Blocks.CAKE).map(BlockStateProperties.BITES, "bite_counter");
//        register(Blocks.BROWN_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM, Blocks.RED_MUSHROOM_BLOCK).transform("huge_mushroom_bits", state -> {
//            int count = 0;
//            if (state.getValue(BlockStateProperties.DOWN)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.UP)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.SOUTH)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.WEST)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.NORTH)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.EAST)) {
//                count++;
//            }
//            return count;
//        });
//        register(Blocks.VINE).transform("vine_direction_bits", state -> {
//            int count = 0;
//            if (state.getValue(BlockStateProperties.UP)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.SOUTH)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.WEST)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.NORTH)) {
//                count++;
//            }
//            if (state.getValue(BlockStateProperties.EAST)) {
//                count++;
//            }
//            return count;
//        });
        register(Blocks.NETHER_WART).directMap(NetherWartBlock.AGE);
        register(Blocks.BREWING_STAND)
                .map(BlockStateProperties.HAS_BOTTLE_0, "brewing_stand_slot_a_bit")
                .map(BlockStateProperties.HAS_BOTTLE_1, "brewing_stand_slot_b_bit")
                .map(BlockStateProperties.HAS_BOTTLE_2, "brewing_stand_slot_c_bit");
        register(Blocks.END_PORTAL_FRAME).map(EndPortalFrameBlock.HAS_EYE, "end_portal_eye_bit");
        register(Blocks.COCOA)
                .directMap(CocoaBlock.AGE)
                .transform(CocoaBlock.FACING, "direction",
                        value -> switch (value) {
                            case WEST -> 1;
                            case NORTH -> 2;
                            case EAST -> 3;
                            default -> 0;
                        });
        register(Blocks.TRIPWIRE_HOOK)
                .map(TripWireHookBlock.ATTACHED, "attached_bit")
                .transform(TripWireHookBlock.FACING, "direction",
                        value -> switch (value) {
                            case WEST -> 1;
                            case NORTH -> 2;
                            case EAST -> 3;
                            default -> 0;
                        })
                .map(TripWireHookBlock.POWERED, "powered_bit");
        register(Blocks.TRIPWIRE)
                // ???
                .addBedrockProperty("suspended_bit", true)
                .map(TripWireBlock.ATTACHED, "attached_bit")
                .map(TripWireBlock.DISARMED, "disarmed_bit")
                .map(TripWireBlock.POWERED, "powered_bit");
        register(CommandBlock.class)
                .map(CommandBlock.CONDITIONAL, "conditional_bit")
                .mapFacingDirection(CommandBlock.FACING);
        register(FlowerPotBlock.class).addBedrockProperty("update_bit", false);
        register(Blocks.CARROTS).map(CarrotBlock.AGE, "growth");
        register(Blocks.POTATOES).map(PotatoBlock.AGE, "growth");
        register(Blocks.BEETROOTS).map(BeetrootBlock.AGE, "growth");
        register(RotatedPillarBlock.class).map(RotatedPillarBlock.AXIS, "pillar_axis");
        register(Blocks.HOPPER)
                .map(HopperBlock.ENABLED, "toggle_bit")
                .transform(HopperBlock.FACING, "facing_direction", value -> switch (value) {
                    case DOWN -> 0;
                    case UP -> throw new IllegalStateException("Unexpected value: " + value);
                    case NORTH -> 2;
                    case SOUTH -> 3;
                    case WEST -> 4;
                    case EAST -> 5;
                });
        // remove duplicates
        register(DoublePlantBlock.class)
            .additionalRequirement(state -> state.getBlock() != Blocks.SEAGRASS && state.getBlock() != Blocks.TALL_SEAGRASS)
            .transform(DoublePlantBlock.HALF, "upper_block_bit", value -> switch (value) {
                case LOWER -> false;
                case UPPER -> true;
            });
        register(BannerBlock.class).map(BannerBlock.ROTATION, "ground_sign_direction");
        register(WallBannerBlock.class).transform(WallBannerBlock.FACING, "facing_direction", direction -> switch (direction) {
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        });
        register(Blocks.CHORUS_FLOWER).directMap(ChorusFlowerBlock.AGE);
        register(Blocks.TORCHFLOWER_CROP).map(TorchflowerCropBlock.AGE, "growth");
        register(Blocks.FROSTED_ICE).directMap(FrostedIceBlock.AGE);
        register(GlazedTerracottaBlock.class).transform(GlazedTerracottaBlock.FACING, "facing_direction", value -> switch (value) {
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        });
        register(Blocks.KELP).map(KelpBlock.AGE, "kelp_age");
        register(Blocks.SNIFFER_EGG, Blocks.TURTLE_EGG)
                .transform(BlockStateProperties.HATCH, "cracked_state", value -> switch (value) {
                    case 0 -> "no_cracks";
                    case 1 -> "cracked";
                    case 2 -> "max_cracked";
                    default -> throw new IllegalStateException("Unexpected value: " + value);
                });
        register(Blocks.TURTLE_EGG)
                .transform(TurtleEggBlock.EGGS, "turtle_egg_count", value -> switch (value) {
                   case 1 -> "one_egg";
                   case 2 -> "two_egg";
                   case 3 -> "three_egg";
                   case 4 -> "four_egg";
                   default -> throw new IllegalStateException("Unexpected value: " + value);
                });
        register(BaseCoralWallFanBlock.class)
                .transform(CoralWallFanBlock.FACING, "coral_direction", value -> switch (value) {
                    case WEST -> 0;
                    case EAST -> 1;
                    case NORTH -> 2;
                    case SOUTH -> 3;
                    default -> throw new IllegalStateException("Unexpected value: " + value);
                });
        register(SeaPickleBlock.class)
                .transform(SeaPickleBlock.WATERLOGGED, "dead_bit", value -> !value)
                .transform(SeaPickleBlock.PICKLES, "cluster_count", value -> value - 1);
//        register(Blocks.BAMBOO)
//                .transform(BambooStalkBlock.LEAVES, "bamboo_leaf_size", value -> switch (value) {
//                    case NONE -> "no_leaves";
//                    case SMALL -> "small_leaves";
//                    case LARGE -> "large_leaves";
//                });
        register(LanternBlock.class).directMap(LanternBlock.HANGING);
        register(CandleCakeBlock.class).directMap(CandleCakeBlock.LIT);
        register(Blocks.RESPAWN_ANCHOR).map(RespawnAnchorBlock.CHARGE, "respawn_anchor_charge");
        register(Blocks.INFESTED_DEEPSLATE).map(BlockStateProperties.AXIS, "pillar_axis");
        register(Blocks.COMPOSTER).map(ComposterBlock.LEVEL, "composter_fill_level");

        // quirky fun bedrock things

        register(Blocks.BAMBOO_SAPLING).addBedrockProperty("age_bit", false);
        register(BaseCoralFanBlock.class)
                .additionalRequirement(state -> !(state.getBlock() instanceof BaseCoralWallFanBlock))
                .addBedrockProperty("coral_fan_direction", 0);
        register(Blocks.QUARTZ_BLOCK,
                Blocks.CHISELED_QUARTZ_BLOCK,
                Blocks.SMOOTH_QUARTZ,
                Blocks.PURPUR_BLOCK)
                .addBedrockProperty("pillar_axis", "y");
        // TODO check
        register(Blocks.KELP_PLANT).addBedrockProperty("kelp_age", 0);
        register(Blocks.SKELETON_SKULL,
                Blocks.DRAGON_HEAD,
                Blocks.PIGLIN_HEAD,
                Blocks.PLAYER_HEAD,
                Blocks.CREEPER_HEAD,
                Blocks.WITHER_SKELETON_SKULL,
                Blocks.ZOMBIE_HEAD)
                .addBedrockProperty("facing_direction", 1);
        register(Blocks.BEDROCK).addBedrockProperty("infiniburn_bit", false);
        register(Blocks.LAVA_CAULDRON).addBedrockProperty("fill_level", 6);
        register(Blocks.BIG_DRIPLEAF_STEM).addBedrockProperty("big_dripleaf_head", true);
        register(Blocks.FLOWER_POT, Blocks.AZALEA_LEAVES).addBedrockProperty("update_bit", false);
        register(LeavesBlock.class).addBedrockProperty("update_bit", false);
        register(Blocks.PUMPKIN).addBedrockProperty("minecraft:cardinal_direction", "north");
        register(Blocks.HAY_BLOCK, Blocks.BONE_BLOCK).addBedrockProperty("deprecated", 0);
    }
}
