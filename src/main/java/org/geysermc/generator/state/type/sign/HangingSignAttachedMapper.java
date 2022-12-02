package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * If the hanging sign is attached, two chains come off the top of the sign and come to a point. If the hanging sign is
 * not attached, the two chains hang completely vertically.
 *
 * If the hanging sign is hanging from a solid block, attached may be true or false, depending if it was placed while
 * crouching. If the hanging sign is hanging from a chain block, attached should be true.
 *
 * If attached is false, the hanging sign is always positioned to face a cardinal direction.
 *
 * Wall hanging signs are mapped differently: {@link WallHangingSignAttachedMapper}
 */
@StateRemapper(value = "attached", blockRegex = {".*hanging_sign.?$", "^((?!wall).)*$"})
public class HangingSignAttachedMapper extends StateMapper<Boolean> {
    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return Pair.of("attached_bit", Boolean.parseBoolean(value));
    }
}
