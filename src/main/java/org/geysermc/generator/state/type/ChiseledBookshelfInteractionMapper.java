package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

// unused JE blockstate value
@StateRemapper(value = "facing", blockRegex = ".*chiseled_bookshelf$")
public class ChiseledBookshelfInteractionMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        return Pair.of("last_interacted_slot", 0); // Can't know and probably doesn't matter for us
    }
}
