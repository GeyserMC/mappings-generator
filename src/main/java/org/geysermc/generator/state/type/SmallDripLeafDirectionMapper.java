package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = "^minecraft:small_dripleaf")
public class SmallDripLeafDirectionMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        // Bedrock's small drip leaves are rotated clockwise once for the same direction, so these values are shifted around
        return Pair.of("minecraft:cardinal_direction", switch (value) {
            case "south" -> "east";
            case "west" -> "south";
            case "north" -> "west";
            case "east" -> "north";
            default -> throw new RuntimeException("Could not determine small dripleaf direction!");
        });
    }
}
