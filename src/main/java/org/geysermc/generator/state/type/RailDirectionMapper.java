package org.geysermc.generator.state.type;

import org.geysermc.generator.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "shape", blockRegex = ".*rail.?$")
public class RailDirectionMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int direction;
        switch (value) {
            case "north_south":
                direction = 0;
                break;
            case "east_west":
                direction = 1;
                break;
            case "ascending_east":
                direction = 2;
                break;
            case "ascending_west":
                direction = 3;
                break;
            case "ascending_north":
                direction = 4;
                break;
            case "ascending_south":
                direction = 5;
                break;
            case "south_east":
                direction = 6;
                break;
            case "south_west":
                direction = 7;
                break;
            case "north_west":
                direction = 8;
                break;
            case "north_east":
                direction = 9;
                break;
            default:
                throw new RuntimeException("Unknown rail state found!: " + value);
        }
        return new Pair<>("rail_direction", direction);
    }
}
