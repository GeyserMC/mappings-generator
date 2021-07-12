package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "powered", blockRegex = "^minecraft:detector_rail|^minecraft:powered_rail|^minecraft:activator_rail")
public class RailPoweredMapper extends StateMapper<Boolean> {
    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return Pair.of("rail_data_bit", value.equals("true"));
    }
}
