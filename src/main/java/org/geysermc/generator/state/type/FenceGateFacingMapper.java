package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*_fence_gate.?$")
public class FenceGateFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction = switch (value) {
            case "north" -> 2;
            case "west" -> 1;
            case "east" -> 3;
            default -> 0;
        };
        return Pair.of("direction", direction);
    }
}
