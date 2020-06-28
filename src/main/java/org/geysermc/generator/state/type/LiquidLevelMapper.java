package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "level", blockRegex = "^minecraft:water|^minecraft:lava")
public class LiquidLevelMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return new Pair<>("liquid_depth", Integer.parseInt(value));
    }
}
