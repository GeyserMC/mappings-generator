package org.geysermc.generator.state.type.wall;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

// Used north value as all walls have it
@StateRemapper(value = "north", blockRegex = ".*_wall.?$")
public class WallBlockTypeMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String trimmedIdentifier = fullIdentifier.split("\\[")[0].split(":")[1];
        // Most walls follow the same naming pattern but not end brick walls
        if (trimmedIdentifier.contains("end_stone_brick")) trimmedIdentifier = "end_brick_wall";
        return Pair.of("wall_block_type", trimmedIdentifier.replace("_wall", ""));
    }
}
