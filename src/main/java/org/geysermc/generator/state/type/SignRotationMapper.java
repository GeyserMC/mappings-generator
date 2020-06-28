package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "rotation", blockRegex = {".*sign.?$", "^((?!wall).)*$"})
public class SignRotationMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return new Pair<>("ground_sign_direction", Integer.parseInt(value));
    }
}
