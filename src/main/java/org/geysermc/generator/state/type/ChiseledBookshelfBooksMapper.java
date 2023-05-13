package org.geysermc.generator.state.type;

import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.generator.state.StateMapper;
import org.geysermc.generator.state.StateRemapper;

@StateRemapper(value = "slot_0_occupied", blockRegex = ".*chiseled_bookshelf$")
public class ChiseledBookshelfBooksMapper extends StateMapper<Integer> {
    @Override
    public Pair<String, Integer> translateState(String fullIdentifier, String value) {
        int totalBooks = 0;
        for (int i = 0; i < 6; i++) {
            String property = "slot_" + i + "_occupied";
            totalBooks += Boolean.parseBoolean(getStateValue(fullIdentifier, property)) ? 1 : 0;
        }

        return Pair.of("books_stored", totalBooks);
    }
}
