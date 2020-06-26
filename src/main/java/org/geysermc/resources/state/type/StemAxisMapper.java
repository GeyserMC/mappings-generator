package org.geysermc.resources.state.type;

import org.geysermc.resources.Pair;
import org.geysermc.resources.state.StateMapper;
import org.geysermc.resources.state.StateRemapper;

@StateRemapper(value = "axis", blockRegex = ".*_stem.?$")
public class StemAxisMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        String axis = "";
        switch (value) {
            case "x":
                axis = "x";
                break;
            case "y":
                axis = "y";
                break;
            case "z":
                axis = "z";
                break;
        }
        return new Pair<>("pillar_axis", axis);
    }
}
