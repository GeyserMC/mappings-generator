package org.geysermc.resources.state.type.wall;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "south", blockRegex = ".*_wall.?$")
public class ConnectionSouthMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String southType;
        switch (value) {
            case "tall":
                southType = "tall";
                break;
            case "low":
                southType = "low";
                break;
            default:
                southType = "none";
                break;
        }
        return new Pair<>("wall_connection_type_south", southType);
    }
}