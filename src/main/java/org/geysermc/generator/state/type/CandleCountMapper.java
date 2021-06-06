package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "candles", blockRegex = ".*candle$")
public class CandleCountMapper extends StateMapper<Integer> {


    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        // todo: don't map candles to pickles and cake candles to cake once Bedrock gets candles
        int additionalPickles = Integer.parseInt(value) - 1;
        return Pair.of("cluster_count", additionalPickles);
    }
}
