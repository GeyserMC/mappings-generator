package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Covers "hanging" for both hanging and wall_hanging signs
 */
@StateRemapper(value = "waterlogged", blockRegex = ".*(wall_)?hanging_sign.?$")
public class HangingSignHangingMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        // wall_hanging signs are "not hanging" on BE.
        // BE only has one identifier for both wall_hanging and hanging signs, so it uses this state to distinguish
        return Pair.of("hanging", !fullIdentifier.contains("wall"));
    }
}
