package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*_door.?$")
public class DoorFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction = switch (value) {
            case "north" -> 3;
            case "west" -> 2;
            case "south" -> 1;
            default -> 0;
        };
        return Pair.of("direction", direction);
    }
}
