package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "can_summon", blockRegex = ".*sculk_shrieker$")
public class SculkShriekerCanSummonMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("can_summon", Boolean.parseBoolean(value) ? 1 : 0);
    }
}
