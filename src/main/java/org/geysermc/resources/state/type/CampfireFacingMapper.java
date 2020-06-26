package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*campfire.?$")
public class CampfireFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction = 0;
        switch (value) {
            case "north":
                direction = 2;
                break;
            case "south":
                direction = 0;
                break;
            case "west":
                direction = 1;
                break;
            case "east":
                direction = 3;
                break;
        }
        return new Pair<>("direction", direction);
    }
}
