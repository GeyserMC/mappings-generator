package org.geysermc.generator.javaclass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import org.geysermc.generator.Util;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import static org.geysermc.generator.javaclass.FieldConstructor.wrap;

public final class GenerateBlocksClass {

    public static void main(String[] args) {
        Util.initialize();

        Map<Block, String> classOverrides = new HashMap<>();
        classOverrides.put(Blocks.PISTON, "PistonBlock");
        classOverrides.put(Blocks.STICKY_PISTON, "PistonBlock");

        List<Class<? extends Block>> mirroredClasses = List.of(BedBlock.class, CauldronBlock.class, ChestBlock.class, DoorBlock.class,
                FlowerPotBlock.class, LecternBlock.class, MovingPistonBlock.class);

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
            }

            for (Class<? extends Block> aClass : mirroredClasses) {
                if (aClass.isAssignableFrom(block.getClass())) {
                    clazz = aClass.getSimpleName();
                    break;
                }
            }

            clazz = classOverrides.getOrDefault(block, clazz);

            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            constructor.declareFieldName(path).declareClassName(clazz);

            switch (block) {
                case AbstractBannerBlock banner -> constructor.addParameter(banner.getColor().getId());
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

            final var properties = block.defaultBlockState().getProperties();
            properties.forEach(property -> {
                switch (property) {
                    case DirectionProperty directionProperty -> {
                        List<String> collection = directionProperty.getPossibleValues().stream().map(direction -> "Direction." + direction.name()).toList();
                        constructor.newline().addMethod("enumState", findFieldName(property), String.join(", ", collection));
                    }
                    case EnumProperty<?> enumProperty -> {
                        if (PropertyBridge.geyserHasEnum(enumProperty.getValueClass())) {
                            Collection<? extends Enum<?>> possibleValues = enumProperty.getPossibleValues();
                            Enum<?>[] allValues = enumProperty.getValueClass().getEnumConstants();
                            Stream<Enum<?>> stream = Arrays.stream(allValues).filter(anEnum -> !possibleValues.contains(anEnum));
                            String result;
                            if (stream.findAny().isPresent()) {
                                // Only some values are present
                                result = String.join(", ", possibleValues.stream().map(value -> enumProperty.getValueClass().getSimpleName() + "." + value.name()).toList());
                            } else {
                                // All values are used
                                result = enumProperty.getValueClass().getSimpleName() + ".VALUES";
                            }
                            constructor.newline().addMethod("enumState", findFieldName(property), result);
                        } else {
                            List<String> collection = enumProperty.getPossibleValues().stream().map(object -> wrap(object.toString().toLowerCase(Locale.ROOT))).toList();
                            constructor.newline().addMethod("enumState", findFieldName(property), String.join(", ", collection));
                        }
                    }
                    case IntegerProperty integerProperty -> {
                        // Replicating the IntegerProperty constructor
                        var values = integerProperty.getPossibleValues();
                        int low = values.stream().min(Comparator.naturalOrder()).orElseThrow();
                        int high = values.stream().max(Comparator.naturalOrder()).orElseThrow();
                        constructor.newline().addMethod("intState", findFieldName(property), Integer.toString(low), Integer.toString(high));
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
