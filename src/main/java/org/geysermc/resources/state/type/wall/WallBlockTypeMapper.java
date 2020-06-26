package org.geysermc.resources.state.type.wall;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

// Used north value as all walls have it
@StateRemapper(value = "north", blockRegex = ".*_wall.?$")
public class WallBlockTypeMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String trimmedIdentifier = fullIdentifier.split("\\[")[0].split(":")[1];
        // Most walls follow the same naming pattern but not end brick walls
        if (trimmedIdentifier.contains("end_stone_brick")) trimmedIdentifier = "end_brick_wall";
        return new Pair<>("wall_block_type", trimmedIdentifier.replace("_wall", ""));
    }
}
