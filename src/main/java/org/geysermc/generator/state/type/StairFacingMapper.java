package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*stairs.?$")
public class StairFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int weirdoDirection = switch (value) {
            case "north" -> 3;
            case "south" -> 2;
            case "west" -> 1;
            default -> 0;
        };
        return Pair.of("weirdo_direction", weirdoDirection);
    }
}
