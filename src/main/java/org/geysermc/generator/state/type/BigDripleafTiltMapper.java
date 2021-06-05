package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "tilt", blockRegex = "^minecraft:big_dripleaf")
public class BigDripleafTiltMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        return Pair.of("big_dripleaf_tilt", switch (value) {
            case "none" -> "none";
            case "unstable" -> "unstable";
            case "partial" -> "partial_tilt";
            case "full" -> "full_tilt";
            default -> throw new RuntimeException("Unknown tilt state for big dripleaf!");
        });
    }
}
