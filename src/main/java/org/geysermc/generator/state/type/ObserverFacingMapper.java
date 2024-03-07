package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = "minecraft:observer$")
public class ObserverFacingMapper extends StateMapper<String> {
    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        // literally the only block with this state key. nice.
        return Pair.of("minecraft:facing_direction", value);
    }
}
