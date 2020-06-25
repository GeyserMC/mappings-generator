package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "rotation", blockRegex = {".*sign.?$", "^((?!wall).)*$"})
public class SignRotationMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return new Pair<>("ground_sign_direction", Integer.parseInt(value));
    }
}
