package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "face", blockRegex = ".*button.?$")
public class ButtonFaceMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        String facing = getStateValue(fullIdentifier, "facing");
        int facingDirection = switch (value) {
            case "floor" -> 1;
            case "wall" -> switch (facing) {
                case "north" -> 2;
                case "south" -> 3;
                case "west" -> 4;
                case "east" -> 5;
                default -> 0;
            };
            default -> 0;
        };
        return Pair.of("facing_direction", facingDirection);
    }
}
