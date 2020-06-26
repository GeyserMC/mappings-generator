package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "in_wall", blockRegex = ".*_fence_gate.?$")
public class FenceGateInWallMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return new Pair<>("in_wall_bit", Boolean.parseBoolean(value));
    }
}
