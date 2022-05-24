package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "stage", blockRegex = ".*mangrove_propagule$")
public class MangrovePropaguleStageMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("propagule_stage", Integer.parseInt(value));
    }
}
