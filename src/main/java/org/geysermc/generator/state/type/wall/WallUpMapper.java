package org.geysermc.generator.state.type.wall;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "up", blockRegex = ".*_wall.?$")
public class WallUpMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return new Pair<>("wall_post_bit", Boolean.parseBoolean(value));
    }
}