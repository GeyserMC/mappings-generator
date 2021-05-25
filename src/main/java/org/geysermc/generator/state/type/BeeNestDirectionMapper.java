package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = "^minecraft:bee_nest|^minecraft:beehive")
public class BeeNestDirectionMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction = switch (value) {
            case "west" -> 1;
            case "north" -> 2;
            case "east" -> 3;
            default -> 0;
        };
        return Pair.of("direction", direction);
    }
}
