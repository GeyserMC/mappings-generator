package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "bloom", blockRegex = ".*sculk_catalyst$")
public class SculkCatalystBloomMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("bloom", Boolean.parseBoolean(value) ? 1 : 0);
    }
}
