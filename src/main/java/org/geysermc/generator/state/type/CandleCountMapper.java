package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "candles", blockRegex = ".*candle$")
public class CandleCountMapper extends StateMapper<Integer> {


    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        // map candles to pickles until bedrock gets them
        int additionalPickles = Integer.parseInt(value) - 1;
        return Pair.of("cluster_count", additionalPickles);
    }
}
