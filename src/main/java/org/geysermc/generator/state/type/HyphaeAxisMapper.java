package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "axis", blockRegex = ".*_hyphae.?$")
public class HyphaeAxisMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        return new Pair<>("pillar_axis", value);
    }
}
