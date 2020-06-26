package org.geysermc.resources.state.type.wall;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "up", blockRegex = ".*_wall.?$")
public class PostBitMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        boolean wallPostBit;
        wallPostBit = "true".equals(value);
        return new Pair<>("wall_post_bit", wallPostBit);
    }
}