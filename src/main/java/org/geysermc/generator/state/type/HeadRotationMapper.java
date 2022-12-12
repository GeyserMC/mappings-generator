package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "rotation", blockRegex = {".*_(head|skull)", "^((?!wall).)*$", "^((?!piston).)*$"})
public class HeadRotationMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("facing_direction", 1); // Handled elsewhere (see getRemapBlock)
    }
}
