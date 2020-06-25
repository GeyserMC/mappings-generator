package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "half", blockRegex = ".*trapdoor.?$")
public class TrapdoorHalfMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return new Pair<>("upside_down_bit", value.equals("top"));
    }
}
