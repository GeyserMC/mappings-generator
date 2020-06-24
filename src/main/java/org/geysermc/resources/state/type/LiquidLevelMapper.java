package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "level", blockRegex = "^minecraft:water|^minecraft:lava")
public class LiquidLevelMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return new Pair<>("liquid_depth", Integer.parseInt(value));
    }
}
