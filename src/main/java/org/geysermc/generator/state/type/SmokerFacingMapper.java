package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = "^minecraft:smoker")
public class SmokerFacingMapper extends StateMapper<String> {
    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        return Pair.of("minecraft:cardinal_direction", value);
    }
}
