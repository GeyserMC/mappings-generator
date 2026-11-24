package org.geysermc.mappings.transformer.instance.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;

import java.util.Set;

// Not mapping Java SoundEvent directly because it conflicts with Cloudburst's SoundEvent
public final class SoundMappings {
    private static final Transformer.Maybe<String, String> JAVA_BLOCKS = Transformer.maybe(builder -> builder
            // Use the same sound
            .transform("cherry_sapling", "bamboo_sapling")
            .transform("gilded_blackstone", "nether_gold_ore")
            .transform("glass", "stone") // Except for breaking, mapped explicitly below

            .flatMapInput(Transformers.registryElement(BuiltInRegistries.BLOCK))
            .mapInput(Block::defaultBlockState)
            .mapInput(Transformers.BLOCKS)
            .tryAll(Transformer.identity()));
    // Bedrock block name -> bedrock block sound name
    private static final Transformer.Maybe<String, String> LEGACY_BLOCKS = Transformer.maybe(builder -> builder
            .maybeTransform(Transformers.findAndReplace("copper_chest_oxidized", "copper_chest.oxidized"))
            .maybeTransform(Transformers.findAndReplace("copper_chest_weathered", "copper_chest.weathered"))
            .maybeTransform(Transformers.findAndReplace("enchantment_table", "enchanting_table"))
            .maybeTransform(Transformers.findAndReplace("flowering_azalea", "azalea"))
            .maybeTransform(Transformers.findAndReplace("hanging_sign", "sign"))
            .maybeTransform(Transformers.findAndReplace("ladder", "wood"))
            .maybeTransform(Transformers.findAndReplace("metal", "stone"))
            .maybeTransform(Transformers.findAndReplace("moss_carpet", "moss"))
            .maybeTransform(Transformers.findAndReplace("noteblock", "note"))
            .maybeTransform(Transformers.findAndReplace("wool", "cloth"))
            .maybeTransform(Transformers.findAndReplace("chorus_flower", "chorusflower"))
            .maybeTransform(Transformers.findAndReplace("end_gateway", "end_portal"))
            .maybeTransform(Transformers.findAndReplace("nether_ore", "nether_gold_ore")));
    // M
    public static final Transformer.MaybeCreator<Set<String>, String, SoundMapping> INSTANCE = validIds -> Transformer.maybe(builder -> builder
            .check(mapping -> mapping.validate(validIds))
            // Playsound mappings
            .mapOutput(SoundMapping::playSound)

            // Explicit mappings have priority over any other and go first

            // Ambient sounds
            .maybeTransform(Transformers.findAndReplace("ambient\\.soul_sand_valley", "ambient.soulsand_valley"))
            .maybeTransform(Transformers.findAndReplace("ambient\\.underwater\\.loop\\.additions[\\w_.]*", "ambient.underwater.additions"))

            // Block sounds
            .mark()
            .flatMapInput(category("block"))

            .transform("anvil.destroy", "random.anvil_break")
            .maybeTransform(multipleToOne("anvil", "random.anvil_land", "land", "place"))
            .transform("anvil.use", "random.anvil_use")
            .transform("beacon.power_select", "beacon.power")
            // Not a perfect mapping - resonate doesn't exist on bedrock
            .maybeTransform(multipleToOne("bell", "block.bell.hit", "resonate", "use"))
            .transform("brewing_stand.brew", "random.potion.brewed")

            .transform("bubble_column.bubble_pop", "bubble.pop")
            .transform("bubble_column.upwards_ambient", "bubble.up")
            .transform("bubble_column.upwards_inside", "bubble.upinside")
            .transform("bubble_column.whirlpool_ambient", "bubble.down")
            .transform("bubble_column.whirlpool_inside", "bubble.downinside")

            // Java uses break sounds for fall/step of saplings
            .maybeTransform(multipleToOne("cherry_sapling", "block.bamboo_sapling.break", "fall", "step"))

            .transform("chest.close", "random.chestclosed")
            .transform("chest.locked", "random.door_open")
            .transform("chest.open", "random.chestopen")

            .transform("conduit.ambient.short", "conduit.short")
            .transform("conduit.attack.target", "conduit.attack")

            // Doors/trapdoors: open_trapdoor.close -> open_trapdoor.copper
            .maybeTransform(Transformers.findAndReplace("([\\w_]+)_((trap)?door)\\.(close|open)", "$4_$2.$1"))
            .transform("copper_trapdoor.close", "open_trapdoor.copper")

            .transform("creaking_heart.hurt", "block.creaking_heart.trail")
            .transform("creaking_heart.spawn", "block.creaking_heart.spawn_mob")
            // Java uses bamboo sapling for crop breaking
            .transform("crop.break", "block.bamboo_sapling.place")
            .transform("dispenser.dispense", "random.click")
            .transform("dispenser.launch", "random.bow")
            .transform("dry_grass.ambient", "block.dry_grass.ambient.attached")

            .transform("ender_chest.close", "random.enderchestclosed")
            .transform("ender_chest.open", "random.enderchestopen")

            .transform("fire.ambient", "fire.fire")
            .transform("glass.break", "random.glass")

            // Java uses break sounds for fall/step of fungi
            .maybeTransform(multipleToOne("fungus", "dig.fungus", "fall", "step"))
            .transform("furnace.fire_crackle", "block.furnace.lit")
            .reset()

            // Simply map any sound that has an exact match on Bedrock
            .tryAll(Transformer.identity())

            // Try without prefix (block., item., etc.: block.stone.break -> stone.break)
            .maybeTransform(Transformers.findAndReplace("^[\\w_]+\\.", ""))

            .mark()
            // Try to map all block names to Bedrock first
            .flatMapInput(Transformers.filter(Transformers.startsWith("block.")).maybeMap(Transformers.findAndReplaceGroup("block\\.([\\w_]+)\\.[\\w_]+", JAVA_BLOCKS)
                    .orElse(Transformer.identity())))
            // Catch any exact matches
            .tryAll(Transformer.identity())
            // Try various other things combined if not
            .tryAllPermutations(Transformer.Maybe::maybeChain,
                    // block.stone.break -> stone.break
                    Transformers.findAndReplace("block\\.", ""),
                    // block.stone.break -> break.stone
                    Transformers.findAndReplace("block\\.([\\w_]+)\\.([\\w_]+)", "$2.$1"),
                    // idle -> ambient
                    Transformers.findAndReplace("idle", "ambient"),
                    // block variants that could use their regular sound
                    Transformers.findAndReplace("polished_", ""),
                    Transformers.findAndReplace("_tiles", ""),
                    Transformers.findAndReplace("_block", ""),
                    Transformers.findAndReplace("_bricks", "_brick"),
                    // synonym names for sound types: break -> dig, close -> closed, etc.
                    Transformers.findAndReplace("break", "dig"),
                    Transformers.findAndReplace("close", "closed"),
                    Transformers.findAndReplace("place", "use"),
                    Transformers.findAndReplace("(insert|pickup)\\.enchanted", "$1"), // chiseled bookshelf
                    Transformers.findAndReplace("transition", "state_change"), // dried ghast
                    // different block names: coral_block -> coral, etc. note that the block has already been mapped to its bedrock name according to Transformers.BLOCKS
                    LEGACY_BLOCKS,
                    // try replacing hit with break sound if hit doesn't exist
                    Transformers.swap("hit", "break"),
                    // if all else fails, try using step sound
                    Transformers.findAndReplace("break|place|hit|fall|slide", "step")
            )
            .reset()

            .transform(Transformers.endsWith(".extinguish"), "random.fizz")

            .mark()
            // Filter for "entity." and rename to "mob." first
            .flatMapInput(Transformers.findAndReplace("^entity\\.", "mob."))
            // Find any exact matches
            .tryAll(Transformer.identity())
            // Item frames turn into blocks
            .maybeTransform(Transformers.findAndReplace("mob\\.(?:glow_)?item_frame\\.([\\w_]+)", "block.itemframe.$1"))
            // Parrot imitate noises
            .maybeTransform(Transformers.findAndReplace("mob\\.parrot\\.imitate\\.([\\w_]+)", "mob.imitate.$1"))
            // firework has no prefix at all, and no _far sounds on bedrock
            .maybeTransform(Transformers.findAndReplace("mob\\.firework_rocket\\.([\\w_]+)", match -> "firework." + match.group(1).replace("_far", "")))
            // squids
            .maybeTransform(Transformers.findAndReplace("squirt", "ink_squirt"))
            // Try permutations for other things
            .tryAllPermutations(Transformer.Maybe::maybeChain,
                    // Try replacing underscores in the mob's name with nothing
                    Transformers.findAndReplaceGroup("mob\\.([\\w_]+)\\.[\\w_]+", Transformers.findAndReplace("_", "")),
                    // Common synonyms for idle/ambient/etc
                    Transformers.swap("idle", "ambient"),
                    Transformers.findAndReplace("loop", "ambient"),
                    Transformers.findAndReplace("ambient", "say"),
                    Transformers.findAndReplace("ambient", "breathe"),
                    Transformers.swap("fall", "land"),
                    Transformers.swap("hurt", "hit"),
                    Transformers.swap("step", "walk"),
                    Transformers.swap("riding", "ride"),
                    // Different mob names: evoker -> evocation_illager, etc.
                    Transformers.findAndReplace("evoker", "evocation_illager"),
                    Transformers.findAndReplace("evoker_fangs", "evocation_fangs"),
                    Transformers.findAndReplace("cow_moody", "moody_cow")
            )
            .reset()

            .maybeTransform(Transformers.findAndReplace("music\\.[a-z_]+\\.", "music.game."))
            .maybeTransform(Transformers.findAndReplace("music_disc\\.", "record."))

            // Explicit entity mappings
            .flatMapInput(Transformers.findAndReplace("^entity\\.", ""))
    );

    private SoundMappings() {}

    private static Transformer.Maybe<String, String> category(String name) {
        return Transformers.findAndReplace("^" + name + "\\.", "");
    }

    private static Transformer.Maybe<String, String> multipleToOne(String base, String to, String... types) {
        return Transformers.findAndReplace(base + "\\.(" + String.join("|", types) + ")", to);
    }
}
