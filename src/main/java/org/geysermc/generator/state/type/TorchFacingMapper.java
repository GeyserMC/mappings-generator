package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "facing", blockRegex = ".*wall_torch.?$")
public class TorchFacingMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String direction = switch (value) {
            case "north" -> "south";
            case "west" -> "east";
            case "south" -> "north";
            case "east" -> "west";
            default -> "";
        };
        return Pair.of("torch_facing_direction", direction);
    }
}
