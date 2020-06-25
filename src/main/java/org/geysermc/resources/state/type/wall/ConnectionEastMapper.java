package org.geysermc.resources.state.type.wall;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "east", blockRegex = ".*_wall.?$")
public class ConnectionEastMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String eastType;
        switch (value) {
            case "tall":
                eastType = "tall";
                break;
            case "low":
                eastType = "low";
                break;
            default:
                eastType = "none";
                break;
        }
        return new Pair<>("wall_connection_type_east", eastType);
    }
}