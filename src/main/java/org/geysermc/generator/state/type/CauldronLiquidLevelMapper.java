package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "level", blockRegex = ".*cauldron.?$")
public class CauldronLiquidLevelMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("fill_level", switch (value) {
            case "1" -> 3;
            case "2" -> 4;
            case "3" -> 6;
            default -> throw new RuntimeException("Unknown cauldron liquid level!");
        });
    }
}
