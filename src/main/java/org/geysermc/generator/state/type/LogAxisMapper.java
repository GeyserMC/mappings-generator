package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "axis", blockRegex = ".*_log$")
public class LogAxisMapper extends StateMapper<String> {
    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        return Pair.of("pillar_axis", value);
    }
}
