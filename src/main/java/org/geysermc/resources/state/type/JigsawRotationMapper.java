package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "orientation", blockRegex = ".*jigsaw.?$")
public class JigsawRotationMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        String rotString = value.split("_")[1];
        int rotation = 0;
        switch (rotString) {
            case "north":
                rotation = 3;
                break;
            case "south":
                rotation = 2;
                break;
            case "west":
                rotation = 1;
                break;
            case "east":
                rotation = 0;
                break;
        }
        return new Pair<>("rotation", rotation);
    }
}
