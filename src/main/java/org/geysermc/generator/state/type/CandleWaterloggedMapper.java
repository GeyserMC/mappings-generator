package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "waterlogged", blockRegex = ".*candle$")
public class CandleWaterloggedMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        // sea pickles dead_bit is true if there is no water
        return Pair.of("dead_bit", !value.equals("true"));
    }
}
