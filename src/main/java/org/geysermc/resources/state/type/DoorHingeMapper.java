package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "hinge", blockRegex = ".*_door.?$")
public class DoorHingeMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return new Pair<>("door_hinge_bit", value.equals("right"));
    }
}
