package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Covers facing_direction for wall signs and wall_hanging signs
 */
@StateRemapper(value = "facing", blockRegex = ".*wall_(hanging_)?sign.?$")
public class WallSignFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int facing = switch (value) {
            case "north" -> 2;
            case "south" -> 3;
            case "west" -> 4;
            case "east" -> 5;
            default -> 0;
        };
        return Pair.of("facing_direction", facing);
    }
}
