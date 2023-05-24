package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "age", blockRegex = "minecraft:pitcher_crop")
public class PitcherCropAgeMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int growth = switch (Integer.parseInt(value)) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 5;
            default -> 7; // 4 -> 7
        };
        return Pair.of("growth", growth);
    }
}
