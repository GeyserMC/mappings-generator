package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*[^f]_stem.?$")
public class StemFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction = switch (value) {
            case "north" -> 2;
            case "west" -> 4;
            case "south" -> 3;
            case "east" -> 5;
            default -> 0;
        };
        return Pair.of("facing_direction", direction);
    }
}
