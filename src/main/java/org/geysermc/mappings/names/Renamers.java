package org.geysermc.mappings.names;

import net.minecraft.world.item.Item;
import org.geysermc.mappings.names.renamers.ItemClassNames;

/// Holds {@link InstanceRenamer}s and {@link TypeRenamer}s for the various game types.
///
/// If an {@link InstanceRenamer} or {@link TypeRenamer} requires a significant size of code, it should deserve its own class in the
/// {@link org.geysermc.mappings.names.renamers} package, with a reference to it here.
public final class Renamers {
    public static final TypeRenamer<Item, String> ITEM_CLASSES = ItemClassNames.INSTANCE;

    private Renamers() {}
}
