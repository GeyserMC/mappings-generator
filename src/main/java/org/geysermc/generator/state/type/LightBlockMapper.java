package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "level", blockRegex = "^minecraft:light")
public class LightBlockMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return new Pair<>("block_light_level", Integer.parseInt(value));
    }
}
