package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "candles", blockRegex = ".*candle$")
public class CandleCountMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        // Java index starts at 1; Bedrock index starts at 0
        return Pair.of("candles", Integer.parseInt(value) - 1);
    }
}
