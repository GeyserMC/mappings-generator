package org.geysermc.mappings.names;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.mappings.names.renamers.BlockClassNames;
import org.geysermc.mappings.names.renamers.BlockNames;
import org.geysermc.mappings.names.renamers.ItemClassNames;
import org.geysermc.mappings.names.renamers.ItemNames;

/// Holds {@link InstanceRenamer}s and {@link TypeRenamer}s for the various game types.
///
/// If an {@link InstanceRenamer} or {@link TypeRenamer} requires a significant size of code, it should deserve its own class in the
/// {@link org.geysermc.mappings.names.renamers} package, with a reference to it here.
public final class Renamers {
    public static final InstanceRenamer<Block, BlockState, String> BLOCKS = BlockNames.INSTANCE;
    public static final TypeRenamer<Block, String> BLOCK_CLASSES = BlockClassNames.INSTANCE;
    public static final InstanceRenamer<Item, Identifier, Identifier> ITEMS = ItemNames.INSTANCE;
    public static final TypeRenamer<Item, String> ITEM_CLASSES = ItemClassNames.INSTANCE;

    private Renamers() {}
}
