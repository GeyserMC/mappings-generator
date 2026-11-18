package org.geysermc.mappings.transformer.instance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;

public final class ItemNames {
    public static final Transformer<Item, Identifier> INSTANCE = Transformer.strong(builder -> builder
            .mapOutput(Identifier::withDefaultNamespace)

            // Conflicts
            .transform(Items.MAP, "empty_map") // Conflicts with filled map
            .transform(Items.MELON, "melon_block") // Conflicts with melon slice
            .transform(Items.NETHER_BRICK, "netherbrick") // This is the item; the block conflicts
            .transform(Items.NETHER_BRICKS, "nether_brick")
            .transform(Items.SNOW, "snow_layer") // Conflicts with snow block
            .transform(Items.SNOW_BLOCK, "snow")
            .transform(Items.STONE_STAIRS, "normal_stone_stairs") // Conflicts with cobblestone stairs
            .transform(Items.COBBLESTONE_STAIRS, "stone_stairs")
            .transform(Items.STONECUTTER, "stonecutter_block") // Conflicts with, surprisingly, the OLD MCPE stonecutter

            // Changed names
            .transform(Items.FROGSPAWN, "frog_spawn")
            .transform(Items.GLOW_ITEM_FRAME, "glow_frame")
            .transform(Items.ITEM_FRAME, "frame")
            .transform(Items.OAK_DOOR, "wooden_door")
            .transform(Items.SHULKER_BOX, "undyed_shulker_box")
            .transform(Items.SMALL_DRIPLEAF, "small_dripleaf_block")
            .transform(Items.COPPER_BLOCK.waxed().unaffected(), "waxed_copper")
            .transform(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, "zombie_pigman_spawn_egg")
            .transform(Items.ROOTED_DIRT, "dirt_with_roots")
            .transform(Items.NETHER_QUARTZ_ORE, "quartz_ore")
            .transform(Items.FLOWERING_AZALEA_LEAVES, "azalea_leaves_flowered")
            .transform(Items.COBWEB, "web")
            .transform(Items.DEAD_BUSH, "deadbush")
            .transform(Items.BRICKS, "brick_block")
            .transform(Items.SPAWNER, "mob_spawner")
            .transform(Items.JACK_O_LANTERN, "lit_pumpkin")
            .transform(Items.LILY_PAD, "waterlily")
            .transform(Items.END_STONE_BRICKS, "end_bricks")
            .transform(Items.LIGHT, "light_block")
            .transform(Items.TERRACOTTA, "hardened_clay")
            .transform(Items.DIRT_PATH, "grass_path")
            .transform(Items.PRISMARINE_BRICK_STAIRS, "prismarine_bricks_stairs") // bruh
            .transform(Items.MAGMA_BLOCK, "magma")
            .transform(Items.RED_NETHER_BRICKS, "red_nether_brick") // bruh v2 - be consistent Mojang!
            .transform(Items.GLAZED_TERRACOTTA.lightGray(), "silver_glazed_terracotta")
            .transform(Items.END_STONE_BRICK_STAIRS, "end_brick_stairs")
            .transform(Items.SLIME_BLOCK, "slime")
            .transform(Items.NOTE_BLOCK, "noteblock")
            .transform(Items.OAK_BUTTON, "wooden_button")
            .transform(Items.OAK_PRESSURE_PLATE, "wooden_pressure_plate")
            .transform(Items.OAK_TRAPDOOR, "trapdoor")
            .transform(Items.OAK_FENCE_GATE, "fence_gate")
            .transform(Items.POWERED_RAIL, "golden_rail")
            .transform(Items.STONE_SLAB, "normal_stone_slab")

            // All of these are 1 item on bedrock
            .instanceOf(BannerItem.class, "banner")
            .instanceOf(MapItem.class, "filled_map")
            .transform(item -> Items.BED.asList().contains(item), "bed")

            // Don't exist on bedrock edition (yet)
            .transform(Items.TEST_BLOCK, "unknown")
            .transform(Items.TEST_INSTANCE_BLOCK, "unknown")

            // Replaced by us
            .transform(Items.KNOWLEDGE_BOOK, "book")
            .transform(Items.SPECTRAL_ARROW, "arrow")
            .transform(Items.TIPPED_ARROW, "arrow")
            .transform(Items.DEBUG_STICK, "stick")
            .transform(Items.FURNACE_MINECART, "hopper_minecart")

            .finish(Transformers.registryKey(BuiltInRegistries.ITEM)));

    private ItemNames() {}
}
