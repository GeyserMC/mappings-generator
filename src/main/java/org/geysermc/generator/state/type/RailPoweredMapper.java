package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "powered", blockRegex = "^minecraft:detector_rail|^minecraft:powered_rail|^minecraft:activator_rail")
public class RailPoweredMapper extends StateMapper<Boolean> {
    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return new Pair<>("rail_data_bit", value.equals("true"));
    }
}
