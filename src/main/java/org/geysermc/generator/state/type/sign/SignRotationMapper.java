package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Covers "ground_sign_direction" for both standing signs, and hanging signs (non-wall variants)
 */
@StateRemapper(value = "rotation", blockRegex = {".*sign.?$", "^((?!wall).)*$"})
public class SignRotationMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("ground_sign_direction", Integer.parseInt(value));
    }
}
