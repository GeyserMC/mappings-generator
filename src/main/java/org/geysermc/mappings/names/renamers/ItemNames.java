package org.geysermc.mappings.names.renamers;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.geysermc.mappings.names.InstanceRenamer;

public final class ItemNames {
    public static final InstanceRenamer<Item, Identifier, Identifier> INSTANCE = InstanceRenamer.of(ItemNames::mapItemIdentifier, builder -> builder
            .mapResult(Identifier::withDefaultNamespace)
            // Conflicts
            .rename(Items.MAP, "empty_map") // Conflicts with filled map
            .rename(Items.MELON, "melon_block") // Conflicts with melon slice
            .rename(Items.NETHER_BRICK, "netherbrick") // This is the item; the block conflicts
            .rename(Items.NETHER_BRICKS, "nether_brick")
            .rename(Items.SNOW, "snow_layer") // Conflicts with snow block
            .rename(Items.SNOW_BLOCK, "snow")
            .rename(Items.STONE_STAIRS, "normal_stone_stairs") // Conflicts with cobblestone stairs
            .rename(Items.COBBLESTONE_STAIRS, "stone_stairs")
            .rename(Items.STONECUTTER, "stonecutter_block") // Conflicts with, surprisingly, the OLD MCPE stonecutter
            // Changed names
            .rename(Items.FROGSPAWN, "frog_spawn")
            .rename(Items.GLOW_ITEM_FRAME, "glow_frame")
            .rename(Items.ITEM_FRAME, "frame")
            .rename(Items.OAK_DOOR, "wooden_door")
            .rename(Items.SHULKER_BOX, "undyed_shulker_box")
            .rename(Items.SMALL_DRIPLEAF, "small_dripleaf_block")
            .rename(Items.COPPER_BLOCK.waxed().unaffected(), "waxed_copper")
            .rename(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, "zombie_pigman_spawn_egg")
            .rename(Items.ROOTED_DIRT, "dirt_with_roots")
            .rename(Items.NETHER_QUARTZ_ORE, "quartz_ore")
            .rename(Items.FLOWERING_AZALEA_LEAVES, "azalea_leaves_flowered")
            .rename(Items.COBWEB, "web")
            .rename(Items.DEAD_BUSH, "deadbush")
            .rename(Items.BRICKS, "brick_block")
            .rename(Items.SPAWNER, "mob_spawner")
            .rename(Items.JACK_O_LANTERN, "lit_pumpkin")
            .rename(Items.LILY_PAD, "waterlily")
            .rename(Items.END_STONE_BRICKS, "end_bricks")
            .rename(Items.LIGHT, "light_block")
            .rename(Items.TERRACOTTA, "hardened_clay")
            .rename(Items.DIRT_PATH, "grass_path")
            .rename(Items.PRISMARINE_BRICK_STAIRS, "prismarine_bricks_stairs") // bruh
            .rename(Items.MAGMA_BLOCK, "magma")
            .rename(Items.RED_NETHER_BRICKS, "red_nether_brick") // bruh v2 - be consistent Mojang!
            .rename(Items.GLAZED_TERRACOTTA.lightGray(), "silver_glazed_terracotta")
            .rename(Items.END_STONE_BRICK_STAIRS, "end_brick_stairs")
            .rename(Items.SLIME_BLOCK, "slime")
            .rename(Items.NOTE_BLOCK, "noteblock")
            .rename(Items.OAK_BUTTON, "wooden_button")
            .rename(Items.OAK_PRESSURE_PLATE, "wooden_pressure_plate")
            .rename(Items.OAK_TRAPDOOR, "trapdoor")
            .rename(Items.OAK_FENCE_GATE, "fence_gate")
            .rename(Items.POWERED_RAIL, "golden_rail")
            // Maps, all of these are 1 item on bedrock
            // FIXME do these require any metadata? This'll make a lot of duplicates
            .rename(Items.OCEAN_MONUMENT_MAP, "filled_map")
            .rename(Items.WOODLAND_MANSION_MAP, "filled_map")
            .rename(Items.BURIED_TRIAL_CHAMBERS_MAP, "filled_map")
            .rename(Items.JUNGLE_PYRAMID_MAP, "filled_map")
            .rename(Items.SWAMP_HUT_MAP, "filled_map")
            .rename(Items.DESERT_VILLAGE_MAP, "filled_map")
            .rename(Items.PLAINS_VILLAGE_MAP, "filled_map")
            .rename(Items.SAVANNA_VILLAGE_MAP, "filled_map")
            .rename(Items.SNOWY_VILLAGE_MAP, "filled_map")
            .rename(Items.TAIGA_VILLAGE_MAP, "filled_map")
            .rename(Items.BURIED_TREASURE_MAP, "filled_map")
            .rename(Items.BURIED_ANCIENT_CITY_MAP, "filled_map")
            .rename(Items.BURIED_MINESHAFT_MAP, "filled_map")
            .rename(Items.DESERT_PYRAMID_MAP, "filled_map")
            .rename(Items.ABANDONED_CAMP_MAP, "filled_map")
            .rename(Items.WARM_OCEAN_RUINS_MAP, "filled_map")
            // Don't exist on bedrock edition (yet)
            .rename(Items.TEST_BLOCK, "unknown")
            .rename(Items.TEST_INSTANCE_BLOCK, "unknown")
            // Replaced by us
            .rename(Items.KNOWLEDGE_BOOK, "book")
            .rename(Items.SPECTRAL_ARROW, "arrow")
            .rename(Items.TIPPED_ARROW, "arrow")
            .rename(Items.DEBUG_STICK, "stick")
            .rename(Items.FURNACE_MINECART, "hopper_minecart"));

    private ItemNames() {}

    private static Identifier mapItemIdentifier(Identifier baseBedrockIdentifier) {
        String bedrock = baseBedrockIdentifier.getPath();

        if (bedrock.endsWith("banner")) { // Don't include banner patterns
            bedrock = "banner";
        } else if (bedrock.endsWith("bed")) {
            bedrock = "bed";
        }

        if (bedrock.startsWith("stone_slab") || bedrock.startsWith("double_stone_slab")) {
            bedrock = bedrock.replace("stone_slab", "stone_block_slab");
        }
        if (bedrock.startsWith("double_stone_block_slab")) {
            bedrock = bedrock.replace("double_stone_block_slab", "stone_block_slab");
        }

        return Identifier.fromNamespaceAndPath(baseBedrockIdentifier.getNamespace(), bedrock);
    }
}
