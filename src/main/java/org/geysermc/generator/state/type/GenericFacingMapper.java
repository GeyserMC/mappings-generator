package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

// todo dear god help me
@StateRemapper(value = "facing", blockRegex = "minecraft:(blast_furnace|furnace|lit_blast_furnace|lit_furnace|lit_smoker|smoker|anvil|chipped_anvil|damaged_anvil|campfire|end_portal_frame|lectern|pink_petals|comparator|repeater|soul_campfire|comparator|repeater|chest|ender_chest|stonecutter|trapped_chest)")
public class GenericFacingMapper extends StateMapper<String> {
    @Override
    public Pair<String, String> translateState(String fullIdentifier, String value) {
        return Pair.of("minecraft:cardinal_direction", value);
    }
}
