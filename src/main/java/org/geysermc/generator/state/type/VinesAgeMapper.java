package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "age", blockRegex = ".*_vines.?$")
public class VinesAgeMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int age = 0;
        try {
            age = Integer.parseInt(value);
        } catch (NumberFormatException ignored) { }
        if (fullIdentifier.contains("weeping")) {
            return Pair.of("weeping_vines_age", age);
        } else if (fullIdentifier.contains("twisting")) {
            return Pair.of("twisting_vines_age", age);
        } else {
            // Cave vines
            return Pair.of("growing_plant_age", age);
        }
    }
}
