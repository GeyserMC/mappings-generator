package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "face", blockRegex = ".*button.?$")
public class ButtonFaceMapper extends StateMapper<Integer> {

    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        String facing = this.getStateValue(fullIdentifier, "facing");
        int facingDirection = 0;
        switch (value) {
            case "floor":
                facingDirection = 1;
                break;
            case "wall":
                switch (facing) {
                    case "north":
                        facingDirection = 2;
                        break;
                    case "south":
                        facingDirection = 3;
                        break;
                    case "west":
                        facingDirection = 4;
                        break;
                    case "east":
                        facingDirection = 5;
                        break;
                }
                break;
            case "ceiling":
                facingDirection = 0;
                break;
        }
        return new Pair<>("facing_direction", facingDirection);
    }
}
