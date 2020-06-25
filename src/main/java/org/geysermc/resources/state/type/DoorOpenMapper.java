package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "open", blockRegex = ".*door.?$")
public class DoorOpenMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return new Pair<>("open_bit", Boolean.parseBoolean(value));
    }
}
