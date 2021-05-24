package org.geysermc.generator.state.type.wall;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "north", blockRegex = ".*_wall.?$")
public class WallNorthMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        if (value.equals("low")) value = "short";
        return Pair.of("wall_connection_type_north", value);
    }
}