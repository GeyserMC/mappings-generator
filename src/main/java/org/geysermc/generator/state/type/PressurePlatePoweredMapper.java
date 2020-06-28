package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "powered", blockRegex = ".*_pressure_plate.?$")
public class PressurePlatePoweredMapper extends StateMapper<Integer> {
    // This property doesn't matter since signal is handled server side.
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return new Pair<>("redstone_signal", 0);
    }
}
