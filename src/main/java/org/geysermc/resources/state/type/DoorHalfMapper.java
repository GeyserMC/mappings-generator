package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "half", blockRegex = ".*_door.?$")
public class DoorHalfMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return new Pair<>("upper_block_bit", value.equals("upper"));
    }
}
