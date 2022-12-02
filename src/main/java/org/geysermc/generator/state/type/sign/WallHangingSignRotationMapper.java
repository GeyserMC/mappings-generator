package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Covers ground_sign_direction for wall_hanging signs
 */
@StateRemapper(value = "facing", blockRegex = ".*wall_hanging_sign.?$")
public class WallHangingSignRotationMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int rotationDirection = switch (value) {
            case "south" -> 0;
            case "west" -> 4;
            case "north" -> 8;
            case "east" -> 12;
            default -> throw new IllegalArgumentException("Got " + value + " instead of a cardinal direction");
        };

        return Pair.of("ground_sign_direction", rotationDirection);
    }
}
