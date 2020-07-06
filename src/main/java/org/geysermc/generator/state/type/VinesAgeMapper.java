package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "age", blockRegex = ".*_vines.?$")
public class VinesAgeMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int age = 0;
        try {
            age = Integer.parseInt(value);
        } catch (NumberFormatException e) { }
        if (fullIdentifier.contains("weeping")) {
            return new Pair<>("weeping_vines_age", age);
        }
        return new Pair<>("twisting_vines_age", age);
    }
}
