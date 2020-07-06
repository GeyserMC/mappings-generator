package org.geysermc.generator.state.type.wall;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "south", blockRegex = ".*_wall.?$")
public class WallSouthMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        if (value.equals("low")) value = "short";
        return new Pair<>("wall_connection_type_south", value);
    }
}