package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "type", blockRegex = ".*slab.?$")
public class SlabTypeMapper extends StateMapper<String> {

    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        // java has "top", "bottom, "double". double slabs in bedrock have a value of "bottom"...
        return Pair.of("minecraft:vertical_half", value.equals("top") ? "top" : "bottom");
    }
}
