package org.geysermc.generator.javaclass;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.properties.*;
import org.geysermc.generator.Util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static org.geysermc.generator.javaclass.FieldConstructor.wrap;

public final class PropertyBridge {
    private static final Set<Class<? extends Enum<?>>> GEYSER_ENUMS = Set.of(
            ChestType.class,
            Direction.Axis.class,
            FrontAndTop.class
    );

    static boolean geyserHasEnum(Class<?> clazz) {
        return GEYSER_ENUMS.contains(clazz);
    }

    static List<String> allDirections(EnumProperty<Direction> property) {
        return property.getPossibleValues().stream().map(direction -> "Direction." + direction.name()).toList();
    }

    static String allEnums(EnumProperty<?> enumProperty) {
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
        return result;
    }

    static List<String> allEnumsAsStrings(EnumProperty<?> enumProperty) {
        return enumProperty.getPossibleValues().stream().map(object -> wrap(object.toString().toLowerCase(Locale.ROOT))).toList();
    }

    /**
     * Generates the Properties class in Geyser. Re-run for new properties or if GEYSER_ENUMS is updated.
     */
    public static void main(String[] args) throws IllegalAccessException {
        Util.initialize();

        Field[] fields = BlockStateProperties.class.getFields();
        for (Field field : fields) {
            if (Property.class.isAssignableFrom(field.getType())) {
                Property<?> property = (Property<?>) field.get(null);
                String className = field.getType().getSimpleName();
                String parameters = "";
                String type = switch (field.getType().getSimpleName()) {
                    case "IntegerProperty" -> {
                        // Replicating the IntegerProperty constructor
                        var values = ((IntegerProperty) property).getPossibleValues();
                        int low = values.stream().min(Comparator.naturalOrder()).orElseThrow();
                        int high = values.stream().max(Comparator.naturalOrder()).orElseThrow();
                        parameters = ", " + low + ", " + high;
                        yield "IntegerProperty";
                    }
                    case "BooleanProperty" -> "BooleanProperty";
                    case "EnumProperty" -> {
                        if (property.getValueClass().equals(Direction.class)) {
                            className = "EnumProperty";
                            parameters = ", " + String.join(", ", allDirections((EnumProperty<Direction>) property));
                            yield "EnumProperty<Direction>";
                        } else if (geyserHasEnum(property.getValueClass())) {
                            className = "EnumProperty";
                            parameters = ", " + allEnums((EnumProperty<?>) property);
                            yield "EnumProperty<" + property.getValueClass().getSimpleName() + ">";
                        } else {
                            className = "BasicEnumProperty";
                            parameters = ", " + String.join(", ", allEnumsAsStrings((EnumProperty<?>) property));
                            yield "BasicEnumProperty";
                        }
                    }
                    default -> throw new IllegalStateException();
                };
                System.out.println("public static final " + type + " " + field.getName() + " = " + className + ".create(" + wrap(property.getName()) + parameters + ");");
            }
        }
    }
}
