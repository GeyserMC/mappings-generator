package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*wall_torch.?$")
public class TorchFacingMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String direction = "";
        switch (value) {
            case "north":
                direction = "south";
                break;
            case "west":
                direction = "east";
                break;
            case "south":
                direction = "north";
                break;
            case "east":
                direction = "west";
                break;
        }
        return new Pair<>("torch_facing_direction", direction);
    }
}
