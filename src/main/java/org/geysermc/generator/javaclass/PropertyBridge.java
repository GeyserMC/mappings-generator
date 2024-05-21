package org.geysermc.generator.javaclass;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import org.geysermc.generator.Util;

import java.lang.reflect.Field;
import java.util.Set;

import static org.geysermc.generator.javaclass.FieldConstructor.wrap;

public final class PropertyBridge {
    private static final Set<Class<? extends Enum<?>>> GEYSER_ENUMS = Set.of(
            ChestType.class,
            Direction.Axis.class,
            FrontAndTop.class
    );

    static boolean geyserHasEnum(Class<? extends Enum<?>> clazz) {
        return GEYSER_ENUMS.contains(clazz);
    }

    /**
     * Generates the Properties class in Geyser. Re-run for new properties or if GEYSER_ENUMS is updated.
     */
    public static void main(String[] args) throws IllegalAccessException {
        Util.initialize();

        Field[] fields = BlockStateProperties.class.getFields();
        for (Field field : fields) {
            if (Property.class.isAssignableFrom(field.getType())) {
                Property property = (Property) field.get(null);
                String type = switch (field.getType().getSimpleName()) {
                    case "IntegerProperty" -> "Integer";
                    case "BooleanProperty" -> "Boolean";
                    case "DirectionProperty" -> "Direction";
                    case "EnumProperty" -> {
                        if (geyserHasEnum(property.getValueClass())) {
                            yield property.getValueClass().getSimpleName();
                        } else {
                            yield "String";
                        }
                    }
                    default -> throw new IllegalStateException();
                };
                System.out.println("public static final Property<" + type + "> " + field.getName() + " = Property.create(" + wrap(property.getName()) + ");");
            }
        }
    }
}
