package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Covers "attached_bit" for wall_hanging signs
 */
@StateRemapper(value = "facing", blockRegex = ".*wall_hanging_sign.?$")
public class WallHangingSignAttachedMapper extends StateMapper<Boolean> {
    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return Pair.of("attached_bit", true);
    }
}
