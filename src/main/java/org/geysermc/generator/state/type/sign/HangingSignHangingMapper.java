package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Covers "hanging" for both wall signs and wall_hanging signs
 */
@StateRemapper(value = "waterlogged", blockRegex = ".*(wall_)?hanging_sign.?$")
public class HangingSignHangingMapper extends StateMapper<Boolean> {
    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return Pair.of("hanging", true); // todo: what is this suppose to be?
    }
}
