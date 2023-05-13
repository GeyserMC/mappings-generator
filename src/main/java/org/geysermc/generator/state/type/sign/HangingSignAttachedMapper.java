package org.geysermc.generator.state.type.sign;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * If the hanging sign is attached, two chains come off the top of the sign and come to a point. If the hanging sign is
 * not attached, the two chains hang completely vertically.
 *
 * The most important thing is that if attached is false, the hanging sign is always facing a cardinal direction.
 *
 * Note that this mapper covers both wall_hanging and hanging signs. attached_bit does not matter for wall_hanging signs,
 * in which the BE state "hanging" is false.
 *
 * Extra information: if the hanging sign is hanging from a solid block, attached may be true or false, depending on if
 * it was placed while crouching. However, if the hanging sign is below a chain, attached is true.
 */
@StateRemapper(value = "attached", blockRegex = {".*hanging_sign.?$"})
public class HangingSignAttachedMapper extends StateMapper<Boolean> {
    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        boolean state;
        if (fullIdentifier.contains("wall")) {
            state = false;
        } else {
            state = Boolean.parseBoolean(value);
        }

        return Pair.of("attached_bit", state);
    }
}
