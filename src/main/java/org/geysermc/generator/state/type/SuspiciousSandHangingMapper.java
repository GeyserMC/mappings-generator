package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Also covers suspicious_gravel
 */
@StateRemapper(value = "dusted", blockRegex = "minecraft:suspicious_(sand|gravel)$")
public class SuspiciousSandHangingMapper extends StateMapper<Boolean> {

    @Override
    public Pair<String, Boolean> translateState(String fullIdentifier, String value) {
        return Pair.of("hanging", false); // seemingly undeterminable
    }
}
