package org.geysermc.generator.javaclass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import org.geysermc.generator.EmptyLevelReader;
import org.geysermc.generator.Util;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

import static org.geysermc.generator.javaclass.FieldConstructor.wrap;

public final class GenerateBlocksClass {

    public static void main(String[] args) {
        Util.initialize();

        Map<Block, String> classOverrides = new HashMap<>();
        classOverrides.put(Blocks.PISTON, "PistonBlock");
        classOverrides.put(Blocks.STICKY_PISTON, "PistonBlock");
        classOverrides.put(Blocks.WATER, "WaterBlock");

        List<Class<? extends Block>> mirroredClasses = List.of(BedBlock.class, CauldronBlock.class, ChestBlock.class, DoorBlock.class,
                FlowerPotBlock.class, FurnaceBlock.class, HoneyBlock.class, LecternBlock.class, MovingPistonBlock.class,
                PistonHeadBlock.class, SpawnerBlock.class, TrapDoorBlock.class, WallSkullBlock.class);

        StringBuilder builder = new StringBuilder();
        var it = BuiltInRegistries.BLOCK.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            FieldConstructor constructor = new FieldConstructor("Block");

            String clazz = "Block";
            if (block instanceof AbstractBannerBlock) {
                clazz = "BannerBlock";
            } else if (block instanceof AbstractSkullBlock) {
                clazz = "SkullBlock";
            } else if (block instanceof AbstractCauldronBlock) {
                clazz = "CauldronBlock";
            }

            boolean classMirrored = false;
            for (Class<? extends Block> aClass : mirroredClasses) {
                if (aClass.isAssignableFrom(block.getClass())) {
                    clazz = aClass.getSimpleName();
                    classMirrored = true;
                    break;
                }
            }

            clazz = classOverrides.getOrDefault(block, clazz);

            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            constructor.declareFieldName(path).declareClassName(clazz).addParameter(wrap(path));

            switch (block) {
                case AbstractBannerBlock banner -> constructor.addParameter(banner.getColor().getId());
                case AbstractSkullBlock skull -> constructor.addParameter("SkullBlock.Type." + ((SkullBlock.Types) skull.getType()).name());
                case BedBlock bed -> constructor.addParameter(bed.getColor().getId());
                case FlowerPotBlock flowerPot ->
                        constructor.addParameter(BuiltInRegistries.BLOCK.getKey(flowerPot.getPotted()).getPath().toUpperCase(Locale.ROOT));
                default -> {
                }
            }

            constructor.finishParameters();

            // As of 1.20.5, all these are unanimous per-block!!
            final BlockState defaultState = block.defaultBlockState();
            if (defaultState.hasBlockEntity()) {
                constructor.addMethod("setBlockEntity");
            }
            if (defaultState.requiresCorrectToolForDrops()) {
                constructor.addMethod("requiresCorrectToolForDrops");
            }
            final float destroyTime = block.defaultDestroyTime();
            if (destroyTime != 0f) {
                constructor.addMethod("destroyTime", destroyTime);
            }
            final PushReaction pushReaction = defaultState.getPistonPushReaction();
            if (pushReaction != PushReaction.NORMAL) {
                constructor.addMethod("pushReaction", "PistonBehavior." + pushReaction);
            }

            // These are not unanimous, but we can figure that out pretty quick.
            if (!classMirrored) {
                var allStates = block.getStateDefinition().getPossibleStates();
                Item item = null;
                for (BlockState state : allStates) {
                    Item currentItem;
                    try {
                        currentItem = block.getCloneItemStack(EmptyLevelReader.INSTANCE, BlockPos.ZERO, state).getItem();
                    } catch (Exception e) {
                        break;
                    }
                    if (item == null) {
                        item = currentItem;
                    } else if (item != currentItem) {
                        // ItemStack changes depending on state - as of 1.20.5 this is *just* piston heads.
                        item = null;
                        break;
                    }
                }
                if (item != null && block.asItem() != item) {
                    constructor.addMethod("pickItem", "() -> Items." + BuiltInRegistries.ITEM.getKey(item).getPath().toUpperCase(Locale.ROOT));
                }
            }

            final var properties = block.defaultBlockState().getProperties();
            properties.forEach(property -> {
                switch (property) {
                    case DirectionProperty directionProperty -> {
                        List<String> collection = PropertyBridge.allDirections(directionProperty);
                        constructor.newline().addMethod("enumState", findFieldName(property), String.join(", ", collection));
                    }
                    case EnumProperty<?> enumProperty -> {
                        if (PropertyBridge.geyserHasEnum(enumProperty.getValueClass())) {
                            constructor.newline().addMethod("enumState", findFieldName(property), PropertyBridge.allEnums(enumProperty));
                        } else {
                            // Geyser's BasicEnumProperty stores the values for BlockState value switching
                            constructor.newline().addMethod("enumState", findFieldName(property));
                        }
                    }
                    case IntegerProperty ignored -> {
                        // Geyser's IntegerProperty has to store the low and high anyway, so we'll get the rates
                        // from there.
                        constructor.newline().addMethod("intState", findFieldName(property));
                    }
                    case BooleanProperty ignored -> {
                        constructor.newline().addMethod("booleanState", findFieldName(property));
                    }
                    default -> throw new IllegalStateException();
                }
            });

            constructor.finish();
            System.out.println(constructor.toString());
            builder.append(constructor.toString());
            if (it.hasNext()) {
                builder.append("\n");
            }
        }
//        System.out.println();
//        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(builder.toString()), null);
//        System.out.println("Contents copied to your clipboard. Your welcome :)");
    }

    private static String findFieldName(Property<?> property) {
        Field[] fields = BlockStateProperties.class.getFields();
        for (Field field : fields) {
            if (Property.class.isAssignableFrom(field.getType())) {
                Property<?> aProperty;
                try {
                    aProperty = (Property<?>) field.get(null);
                } catch (Exception e) {
                    throw new IllegalStateException();
                }
                if (property == aProperty) {
                    return field.getName();
                }
            }
        }
        throw new IllegalStateException();
    }
}
