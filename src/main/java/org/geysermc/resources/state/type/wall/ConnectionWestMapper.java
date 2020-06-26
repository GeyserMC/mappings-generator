package org.geysermc.resources.state.type.wall;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "west", blockRegex = ".*_wall.?$")
public class ConnectionWestMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String westType;
        switch (value) {
            case "tall":
                westType = "tall";
                break;
            case "low":
                westType = "low";
                break;
            default:
                westType = "none";
                break;
        }
        return new Pair<>("wall_connection_type_west", westType);
    }
}