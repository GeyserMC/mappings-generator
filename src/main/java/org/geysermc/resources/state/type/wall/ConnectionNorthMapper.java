package org.geysermc.resources.state.type.wall;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "north", blockRegex = ".*_wall.?$")
public class ConnectionNorthMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String northType;
        switch (value) {
            case "tall":
                northType = "tall";
                break;
            case "low":
                northType = "low";
                break;
            default:
                northType = "none";
                break;
        }
        return new Pair<>("wall_connection_type_north", northType);
    }
}