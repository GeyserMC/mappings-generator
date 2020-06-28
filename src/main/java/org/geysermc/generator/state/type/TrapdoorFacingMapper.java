package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*trapdoor.?$")
public class TrapdoorFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction = 0;
        switch (value) {
            case "north":
                direction = 3;
                break;
            case "south":
                direction = 2;
                break;
            case "west":
                direction = 1;
                break;
            case "east":
                direction = 0;
                break;
        }
        return new Pair<>("direction", direction);
    }
}
