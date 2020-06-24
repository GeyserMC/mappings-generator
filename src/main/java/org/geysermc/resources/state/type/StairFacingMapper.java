package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*stairs.?$")
public class StairFacingMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int weirdoDirection = 0;
        switch (value) {
            case "north":
                weirdoDirection = 3;
                break;
            case "south":
                weirdoDirection = 2;
                break;
            case "west":
                weirdoDirection = 1;
                break;
            case "east":
                weirdoDirection = 0;
                break;
        }
        return new Pair<>("weirdo_direction", weirdoDirection);
    }
}
