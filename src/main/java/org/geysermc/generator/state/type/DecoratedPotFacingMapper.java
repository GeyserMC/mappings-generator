package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = "minecraft:decorated_pot")
public class DecoratedPotFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        // north/south and east/west are switched here compared to other blocks that use "facing"
        int direction = switch (value) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            case "west" -> 3;
            default -> throw new IllegalArgumentException("Got " + value + " instead of a cardinal direction");
        };
        return Pair.of("direction", direction);
    }
}
