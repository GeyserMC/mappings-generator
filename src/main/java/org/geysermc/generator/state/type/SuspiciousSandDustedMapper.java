package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

/**
 * Also covers suspicious_gravel
 */
@StateRemapper(value = "dusted", blockRegex = "minecraft:suspicious_(sand|gravel)$")
public class SuspiciousSandDustedMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("brushed_progress", Integer.parseInt(value));
    }
}
