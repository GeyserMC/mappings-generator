package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*_stem.?$")
public class StemFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction = 0;
        switch (value) {
            case "north":
                direction = 2;
                break;
            case "west":
                direction = 4;
                break;
            case "south":
                direction = 3;
                break;
            case "east":
                direction = 5;
                break;
        }
        return new Pair<>("facing_direction", direction);
    }
}
