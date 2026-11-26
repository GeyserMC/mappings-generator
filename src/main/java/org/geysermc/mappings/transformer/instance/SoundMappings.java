package org.geysermc.mappings.transformer.instance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.geysermc.mappings.definitions.sound.SoundMapping;
import org.geysermc.mappings.transformer.Transformer;
import org.geysermc.mappings.transformer.Transformers;

import java.util.Set;
import java.util.regex.Pattern;

/// Geyser's sound mappings, mapping a Java sound event identifier to its Bedrock counterpart. They differ a lot
/// across editions, so this class is quite the monstrosity. We try to map sounds automatically as much as possible,
/// by trying various permutations and matching them against the list of Bedrock sounds.
///
/// Generally, sound events on both additions are formatted as `<category>.<name>.<type>`, for example, `entity.ender_dragon.growl`, where:
/// - `entity` is the category.
/// - `ender_dragon` is the name.
/// - `growl` is the (sound) type.
///
/// This vocabulary is used across this class.
///
/// Since this class is so big, here's a table of contents to keep it clear for you:
///
/// - {@link #BLOCK_NAMES}: Java -> Bedrock block sound names.
/// - {@link #ENTITY_NAMES}: Java -> Bedrock entity sound names.
/// - {@link #ENTITY_TYPES}: Java -> Bedrock entity sound types. Depends on the sound name.
/// - {@link #ITEM_NAMES}: Java -> Bedrock item sound names.
/// - {@link #ITEM_TYPES}: Java -> Bedrock item sound types. Depends on the sound name.
/// - {@link #INSTANCE}: the actual sound mappings, all in one {@link Transformer}. This is a long one:
///   - Explicit block sound mappings.
///   - Explicit entity sound mappings.
///   - Explicit item sound mappings.
///   - Other explicit sound mappings.
///   - Block sound transformers.
///   - Entity sound transformers.
///   - Item sound transformers.
///   - Other sound transformers.
public final class SoundMappings {
    /// Java block sound name -> Bedrock block sound name
    private static final Transformer<String, String> BLOCK_NAMES = Transformer.of(builder -> builder
            .transform("chorus_flower", "chorusflower")
            .transform("copper_chest_oxidized", "copper_chest.oxidized")
            .transform("copper_chest_weathered", "copper_chest.weathered")
            .transform("enchantment_table", "enchanting_table")
            .transform("end_gateway", "end_portal")
            .transform("nether_ore", "nether_gold_ore")
            .transform("note_block", "note")
            // Bedrock uses the same name as Java for sounds, but not for the actual block
            .transform("snow", "snow")
            .transform("vine", "vines")
            .transform("wart_block", "nether_wart")
            // Sounds similar. wet_grass = kelp, seagrass, etc.
            .transform("wet_grass", "coral")
            .transform("wool", "cloth")
            // Neat little trick to map all the other ones using the block names transformer we already have
            .maybeTransform(Transformers.registryElement(BuiltInRegistries.BLOCK)
                    .maybeMap(Block::defaultBlockState).maybeMap(Transformers.BLOCKS))
            .finish(Transformer.identity()));

    /// Java entity sound name -> Bedrock entity sound name
    private static final Transformer<String, String> ENTITY_NAMES = Transformer.of(builder -> builder
            .transform("cat_royal", "royal_cat")
            .transform("chicken_picky", "picky_chicken")
            .transform("copper_golem_oxidized", "copper_golem.oxidized")
            .transform("copper_golem_weathered", "copper_golem.weathered")
            .transform("cow_moody", "moody_cow")
            .transform("donkey", "horse.donkey")
            .transform("elder_guardian", "elderguardian")
            .transform("ender_dragon", "enderdragon")
            .transform("enderman", "endermen")
            .transform("evoker", "evocation_illager")
            .transform("evoker_fangs", "evocation_fangs")
            .transform("illusioner", "evocation_illager") // Doesn't exist on bedrock
            .transform("iron_golem", "irongolem")
            .transform("magma_cube", "magmacube")
            .transform("pig_big", "big_pig")
            .transform("pig_mini", "mini_pig")
            .transform("polar_bear", "polarbear")
            .transform("shulker_bullet", "shulker.bullet")
            .transform("skeleton_horse", "horse.skeleton")
            .transform("small_sulfur_cube", "sulfur_cube.small")
            .transform("snow_golem", "snowgolem")
            .transform("wandering_trader", "wanderingtrader")
            .transform("zombie_horse", "horse.zombie")
            // Wolves are a special case: wolf_angry turns into wolf.mad, wolf_big into wolf.big, etc.
            .maybeTransform(Transformers.findAndReplace("wolf_(angry|big|cute|grumpy|puglin|sad)", match -> {
                String variant = match.group(1);
                if (variant.equals("angry")) {
                    variant = "mad";
                }
                return "wolf." + variant;
            }))
            .finish(Transformer.identity()));
    /// Java entity sound type -> Bedrock entity sound type (depending on input, which is Bedrock entity sound name)
    private static final Transformer.Creator<String, String, String> ENTITY_TYPES = name -> Transformer.of(builder -> builder
            // Cat (not baby or royal)
            .root().flatMapInput(filterForName(name, "cat"))
            .transform("ambient", "meow")
            .transform("stray_ambient", "straymeow")
            // Chicken
            .root().flatMapInput(filterForName(name, "chicken"))
            .transform("egg", "plop")
            // Copper golem
            .root().flatMapInput(filterForName(name, "copper_golem"))
            .transform("item_drop", "chest_interaction.put_item")
            .transform("item_no_drop", "chest_interaction.put_item_fail")
            .transform("no_item_get", "chest_interaction.take_item")
            .transform("no_item_no_get", "chest_interaction.take_item_fail")
            // Enderman
            .root().flatMapInput(filterForName(name, "endermen"))
            .transform("teleport", "portal")
            // Frog/goat
            .root().flatMapInput(filterForName(name, "frog|goat"))
            .transform("long_jump", "jump_to_block")
            // Ghast (not happy or ghastling)
            .root().flatMapInput(filterForName(name, "ghast"))
            .transform("ambient", "moan")
            .transform("hurt", "scream")
            .transform("shoot", "fireball")
            .transform("warn", "charge")
            // Happy ghast
            .root().flatMapInput(filterForName(name, "happy_ghast"))
            .transform("equip", "harness_equip")
            .transform("unequip", "harness_unequip")
            .maybeTransform(Transformers.findAndReplace("harness_", ""))
            // Horse (just horse, not any variants)
            .root().flatMapInput(filterForName(name, "horse"))
            .transform("saddle", "leather")
            .transform("step", "soft")
            .transform("step_wood", "wood")
            // Husk
            .root().flatMapInput(filterForName(name, "husk"))
            .transform("converted_to_zombie", "convert_to_zombie")
            // Iron golem
            .root().flatMapInput(filterForName(name, "irongolem"))
            .transform("attack", "throw")
            .transform("damage", "crack")
            // Magma cube/slime
            .root().flatMapInput(filterForName(name, "magmacube|slime"))
            .transform("squish", "big")
            .transform("squish_small", "small")
            // Panda
            .root().flatMapInput(filterForName(name, "panda"))
            .transform("aggressive_ambient", "idle.aggressive")
            .transform("pre_sneeze", "presneeze")
            .transform("worried_ambient", "idle.worried")
            // bunny
            .root().flatMapInput(filterForName(name, "rabbit"))
            .transform("jump", "hop")
            // Ravager
            .root().flatMapInput(filterForName(name, "ravager"))
            .transform("attack", "bite")
            .transform("stunned", "stun")
            // Skeleton
            .root().flatMapInput(filterForName(name, "skeleton"))
            .transform("converted_to_stray", "convert_to_stray")
            // Sniffer
            .root().flatMapInput(filterForName(name, "sniffer"))
            .transform("digging_stop", "stand_up")
            .transform("scenting", "sniffsniff")
            .transform("sniffing", "long_sniff")
            // Strider
            .root().flatMapInput(filterForName(name, "strider"))
            .transform("happy", "tempt")
            .transform("retreat", "panic")
            // Sulfur cube (and small variant)
            .root().flatMapInput(filterForName(name, "sulfur_cube(\\.small)?"))
            .transform("squish", "landing")
            // Tadpole
            .root().flatMapInput(filterForName(name, "tadpole"))
            .transform("grow_up", "convert_to_frog")
            // Turtle
            .root().flatMapInput(filterForName(name, "turtle"))
            .transform("shamble", "step")
            .transform("shamble_baby", "step_baby")
            // Villager/wandering trader
            .root().flatMapInput(filterForName(name, "villager|wanderingtrader"))
            .transform("trade", "haggle")
            // Warden
            .root().flatMapInput(filterForName(name, "warden"))
            .transform("attack_impact", "attack")
            .transform("tendril_clicks", "clicking")
            // Wolf, including baby and variants
            .root().flatMapInput(filterForName(name, "(baby_)?wolf(\\.[\\w_]+)?"))
            .transform("ambient", "bark")
            .transform("pant", "panting")
            // Zombie
            .root().flatMapInput(filterForName(name, "zombie"))
            .transform("attack_wooden_door", "wood")
            .transform("break_wooden_door", "woodbreak")
            .finish(Transformer.identity()));

    /// Java item sound name -> Bedrock item sound name
    private static final Transformer<String, String> ITEM_NAMES = Transformer.of(builder -> builder
            .transform("spear_wood", "wooden_spear")
            .finish(Transformer.identity()));
    /// Java item sound type -> Bedrock item sound type (depending on input, which is Bedrock item sound name)
    private static final Transformer.Creator<String, String, String> ITEM_TYPES = name -> Transformer.of(builder -> builder
            // Bottle
            .root().flatMapInput(filterForName(name, "bottle"))
            .transform("fill_dragonbreath", "dragonbreath")
            // Bucket
            .root().flatMapInput(filterForName(name, "bucket"))
            .transform("empty", "empty_water")
            .transform("fill", "fill_water")
            .transform("fill_axolotl", "fill_fish") // Doesn't exist on Bedrock?
            // Crossbow
            .root().flatMapInput(filterForName(name, "crossbow"))
            .transform("loading_end", "loading.end")
            .transform("loading_middle", "loading.middle")
            .transform("loading_start", "loading.start")
            .transform("quick_charge_1", "quick_charge.start")
            .transform("quick_charge_2", "quick_charge.middle")
            .transform("quick_charge_3", "quick_charge.end")
            // Elytra
            .root().flatMapInput(filterForName(name, "elytra"))
            .transform("flying", "loop")
            // Lodestone compass
            .root().flatMapInput(filterForName(name, "lodestone_compass"))
            .transform("lock", "link_compass_to_lodestone")
            // Mace
            .root().flatMapInput(filterForName(name, "mace"))
            .transform("smash_ground_heavy", "heavy_smash_ground")
            // Ominous bottle
            .root().flatMapInput(filterForName(name, "ominous_bottle"))
            .transform("dispose", "end_use")
            // Spear including wooden
            .root().flatMapInput(filterForName(name, "(wooden_)?spear"))
            .transform("attack", "attack_hit")
            .finish(Transformer.identity()));

    public static final Transformer.MaybeCreator<Set<String>, String, SoundMapping> INSTANCE = validIds -> Transformer.maybe(builder -> builder
            .check(mapping -> mapping.validate(validIds))
            // Playsound mappings
            .mapOutput(SoundMapping::playSound)

            // Explicit mappings have priority over any other and go first

            .mark() // Start block sounds
            .flatMapInput(category("block"))

            .transform("anvil.destroy", "random.anvil_break")
            .transform("anvil.land", "random.anvil_land")
            .transform("anvil.use", "random.anvil_use")

            .transform("beacon.power_select", "beacon.power")
            .transform("bell.use", "block.bell.hit")
            .transform("brewing_stand.brew", "random.potion.brewed")

            .transform("bubble_column.bubble_pop", "bubble.pop")
            .transform("bubble_column.upwards_ambient", "bubble.up")
            .transform("bubble_column.upwards_inside", "bubble.upinside")
            .transform("bubble_column.whirlpool_ambient", "bubble.down")
            .transform("bubble_column.whirlpool_inside", "bubble.downinside")

            .transform("chest.close", "random.chestclosed")
            .transform("chest.locked", "random.door_open")
            .transform("chest.open", "random.chestopen")

            .transform("conduit.ambient.short", "conduit.short")
            .transform("conduit.attack.target", "conduit.attack")

            .transform("copper_trapdoor.close", "open_trapdoor.copper")
            .transform("creaking_heart.hurt", "block.creaking_heart.trail")
            .transform("creaking_heart.spawn", "block.creaking_heart.spawn_mob")
            .transform("dispenser.dispense", "random.click")
            .transform("dispenser.launch", "random.bow")
            .transform("dry_grass.ambient", "block.dry_grass.ambient.attached")
            .transform("ender_chest.close", "random.enderchestclosed")
            .transform("ender_chest.open", "random.enderchestopen")
            .transform("fire.ambient", "fire.fire")
            .transform("furnace.fire_crackle", "block.furnace.lit")
            .transform("glass.break", "random.glass")
            .transform("lava.ambient", "liquid.lava")
            .transform("lava.pop", "random.pop")
            .transform("lever.click", "random.lever_click")
            .transform("piston.contract", "tile.piston.in")
            .transform("piston.extend", "tile.piston.out")
            .transform("portal.ambient", "portal.portal")

            .transform("potent_sulfur.geyser_continuous_eruption", "block.potent_sulfur.geyser_continuous_eruption_start")
            .transform("potent_sulfur.geyser_eruption", "block.potent_sulfur.geyser_eruption_start")
            .transform("potent_sulfur.noxious_gas", "block.potent_sulfur.ambient")

            .transform("redstone_torch.burnout", "random.fizz")
            .transform("sculk_sensor.clicking", "power.on.sculk_sensor")
            .transform("sculk_sensor.clicking_stop", "power.off.sculk_sensor")

            .transform("shulker_box.close", "random.shulkerboxclosed")
            .transform("shulker_box.open", "random.shulkerboxopen")
            .transform("sniffer_egg.plop", "mob.sniffer.plop")
            // Funny enough cave_vines.pick_berries on Java aliases to sweet berry bush, on Bedrock it's the other way around
            .transform("sweet_berry_bush.pick_berries", "pick_berries.cave_vines")
            .transform("tripwire.detach", "random.bowhit")
            .transform("water.ambient", "liquid.water")
            .transform("wet_sponge.dries", "random.fizz")

            .reset() // End block sounds
            .mark() // Start entity sounds
            .flatMapInput(category("entity"))

            .transform("allay.ambient_with_item", "mob.allay.idle_holding")
            .transform("allay.ambient_without_item", "mob.allay.idle")

            .transform("arrow.hit", "random.bowhit")
            .transform("arrow.hit_player", "random.orb")
            .transform("arrow.shoot", "random.bow")

            .transform("axolotl.idle_air", "mob.axolotl.idle")
            .transform("baby_horse.step", "mob.horse.soft") // Doesn't exist on Bedrock?
            .transform("bee.loop_aggressive", "mob.bee.aggressive")
            .transform("breeze.wind_burst", "breeze_wind_charge.burst")
            .transform("copper_golem_become_statue", "mob.copper_golem.becoming_statue")
            .transform("creeper.hurt", "mob.creeper.say")
            .transform("creeper.primed", "random.fuse")
            .transform("dragon_fireball.explode", "random.explode")

            .transform("elder_guardian.ambient_land", "mob.guardian.land_idle")
            .transform("elder_guardian.death_land", "mob.guardian.land_death")
            .transform("elder_guardian.hurt_land", "mob.guardian.land_hit")

            .transform("experience_orb.pickup", "random.orb")
            .transform("fishing_bobber.splash", "random.splash")

            .transform("generic.big_fall", "damage.fallbig")
            .transform("generic.burn", "random.fizz")
            .transform("generic.death", "game.player.die")
            .transform("generic.drink", "random.drink")
            .transform("generic.eat", "random.eat")
            .transform("generic.explode", "random.explode")
            .transform("generic.extinguish_fire", "random.fizz")
            .transform("generic.hurt", "random.hurt")
            .transform("generic.small_fall", "damage.fallsmall")
            .transform("generic.swim", "random.swim")

            .transform("goat.long_jump", "component.jump_to_block") // ???
            .transform("goat.screaming.long_jump", "component.jump_to_block") // ???
            .transform("item.break", "random.break")
            .transform("item.pickup", "random.pop")
            .transform("lightning_bolt.impact", "ambient.weather.lightning.impact")
            .transform("lightning_bolt.thunder", "ambient.weather.thunder")

            .transform("minecart.inside", "minecart.inside")
            .transform("minecart.inside.underwater", "minecart.inside")
            .transform("minecart.riding", "minecart.base")

            .transform("mooshroom.milk", "mob.mooshroom.suspicious_milk")
            .transform("pig.hurt", "mob.pig.say")

            .transform("player.attack.crit", "game.player.attack.critical")
            .transform("player.attack.nodamage", "game.player.attack.nodamage")
            .transform("player.attack.strong", "game.player.attack.strong")
            .transform("player.attack.weak", "game.player.attack.nodamage")
            .transform("player.burp", "random.burp")
            .transform("player.hurt", "game.player.hurt")
            .transform("player.hurt_sweet_berry_bush", "block.sweet_berry_bush.hurt")
            .transform("player.levelup", "random.levelup")
            .transform("player.splash", "random.splash")
            .transform("player.swim", "random.swim")

            .transform("polar_bear.ambient_baby", "mob.polarbear_baby.idle")
            .transform("sheep.death", "mob.sheep.say")
            .transform("sheep.hurt", "mob.sheep.say")
            .transform("shulker.hurt_closed", "random.shulkerboxclosed")
            .transform("skeleton_horse.swim", "random.swim")
            .transform("spider.hurt", "mob.spider.say")

            .transform("turtle.ambient_land", "mob.turtle.ambient")
            .transform("turtle.egg_hatch", "mob.turtle_baby.born")
            .transform("turtle.lay_egg", "block.turtle_egg.drop")

            .transform("villager.work_leatherworker", "cauldron.adddye")
            .transform("villager.work_mason", "ui.stonecutter.take_result")
            .transform("wind_charge.wind_burst", "wind_charge.burst")
            .transform("zombie_villager.converted", "mob.zombie.unfect")
            .transform("zombie_villager.cure", "mob.zombie.remedy")

            .reset() // End entity sounds
            .mark() // Start item sounds
            .flatMapInput(category("item"))

            .transform("armor.unequip_nautilus", "mob.nautilus.armor_unequip")
            .transform("axe.scrape", "scrape") // Yes, it's really just this.
            .transform("axe.strip", "use.wood")
            .transform("axe.wax_off", "copper.wax.off")
            .transform("bucket.empty_sulfur_cube", "item.sulfur_cube.empty")
            .transform("bucket.fill_sulfur_cube", "item.sulfur_cube.fill")
            .transform("crossbow.hit", "random.bowhit")
            .transform("dye.use", "sign.dye.use")
            .transform("flintandsteel.use", "fire.ignite")
            .transform("golden_dandelion.unuse", "mob.reset_growth")
            .transform("golden_dandelion.use", "mob.pause_growth")
            .transform("hoe.till", "use.gravel")
            .transform("honey_bottle.drink", "random.drink_honey")
            .transform("honeycomb.wax_on", "copper.wax.on")
            .transform("horse_armor.unequip", "mob.horse.horse_armor_unequip")
            .transform("ink_sac.use", "sign.ink_sac.use")
            .transform("lead.tied", "leashknot.place")
            .transform("lead.untied", "leashknot.break")
            .transform("llama_carpet.unequip", "mob.llama.carpet_unequip")
            .transform("nautilus_saddle_equip", "mob.nautilus.saddle")
            .transform("nautilus_saddle_underwater_equip", "mob.nautilus.saddle_underwater")
            .transform("nether_wart.plant", "place.nether_wart")
            .transform("saddle.unequip", "mob.unsaddle")
            .transform("shovel.flatten", "use.grass")
            .transform("totem.use", "random.totem")

            .reset() // End item sounds

            // Other random explicit mappings

            .transform("enchant.thorns.hit", "damage.thorns")
            .transform("event.raid.horn", "raid.horn")

            .transform("music.creative", "music.game.creative")
            .transform("music.credits", "music.game.credits")
            .transform("music.dragon", "music.game.endboss")
            .transform("music.end", "music.game.end")
            .transform("music.nether.soul_sand_valley", "music.game.soulsand_valley")
            .transform("music.overworld.badlands", "music.overworld.mesa")
            .transform("music.overworld.sparse_jungle", "music.overworld.jungle_edge")
            .transform("music.overworld.swamp", "music.game.swamp_music")
            .transform("music.under_water", "music.game.water")

            .transform("ui.button.click", "random.click")
            .transform("ui.hud.bubble_pop", "hud.bubble.pop")
            .transform("ui.toast.in", "random.toast_recipe_unlocking_in")
            .transform("ui.toast.out", "random.toast_recipe_unlocking_out")

            .transform("weather.end_flash", "ambient.weather.the_end_light_flash")
            .transform("weather.rain", "ambient.weather.rain")
            .transform("weather.rain.above", "ambient.weather.rain")

            // Now, simply map any sound that has an exact match on Bedrock
            .tryAll(Transformer.identity())

            // All left-over sounds at this point will need some kind of transformer to be mapped
            // We have transformers for block, entity and item sounds, and then a few smaller ones at the end

            .mark() // Start block sound transformers
            // First map the block names to their Bedrock counterpart
            .flatMapInput(Transformers.findAndReplaceGroup("^block\\.([\\w_]+)\\.[\\w_.]+$", BLOCK_NAMES.maybe()))
            // Catch any exact matches
            .tryAll(Transformer.identity())

            // Some explicit transformers

            // The drip: block.pointed_dripstone.drip_water_into_cauldron -> cauldron_drip.water.pointed_dripstone
            .maybeTransform(Transformers.findAndReplace("block\\.([\\w_]+)\\.drip_(water|lava)_into_cauldron", "cauldron_drip.$2.$1"))
            .maybeTransform(Transformers.findAndReplace("block\\.([\\w_]+)\\.drip_(water|lava)$", "drip.$2.$1"))
            // Button clicks, simple enough
            .transform(Transformers.contains("stone_button\\.click_(off|on)"), "random.click")
            .transform(Transformers.contains("wooden_button\\.click_(off|on)"), "random.wood_click")
            // Tripwire clicks, pretty much the same
            .transform(Transformers.contains("trip_wire"), "random.click")
            // Doors/trapdoors: copper_trapdoor.open -> open_trapdoor.copper
            .maybeTransform(Transformers.findAndReplace("block\\.([\\w_]+)_((trap)?door)\\.(close|open)", "$4_$2.$1"))

            // Try various other things combined if not matched yet
            .tryAllPermutations(Transformer.Maybe::maybeChain,
                    // block.stone.break -> stone.break
                    Transformers.findAndReplace("^block\\.", ""),
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
                    Transformers.findAndReplace("bounce", "land"),
                    Transformers.findAndReplace("(insert|pickup)\\.enchanted", "$1"), // chiseled bookshelf
                    Transformers.findAndReplace("transition", "state_change"), // dried ghast
                    Transformers.findAndReplace("ominous_activate", "charge_activate"), // trial spawner
                    Transformers.findAndReplace("basedrum", "bd"), // note block
                    // try with a different block name: important for some blocks which use some of their own sounds and some of others
                    Transformers.findAndReplace("trial_spawner", "ominous_item_spawner"),
                    // try replacing hit with break sound if hit doesn't exist
                    Transformers.swap("hit", "break"),
                    // if all else fails, try using step sound
                    Transformers.findAndReplace("break|place|hit|fall|slide", "step")
            )
            .reset() // End block sound transformers

            .mark() // Start entity sound transformers
            // First map "entity." to "mob.", and use ENTITY_NAMES and ENTITY_TYPES to map sound names and types
            .flatMapInput(Transformers.findAndReplace("^entity\\.", "mob.")
                    .maybeChain(Transformers.findAndReplaceGroups("mob\\.([\\w_]+)\\.([\\w_.]+)", ENTITY_NAMES.maybe(), ENTITY_TYPES.maybe())))
            // Some explicit transformers

            // Item frames/paintings turn into blocks
            .maybeTransform(Transformers.findAndReplace("mob\\.(?:glow_)?item_frame\\.([\\w_]+)", "block.itemframe.$1"))
            .maybeTransform(Transformers.findAndReplace("mob\\.painting\\.([\\w_]+)", "block.painting.$1"))
            // Parrot imitate noises
            .maybeTransform(Transformers.findAndReplace("mob\\.parrot\\.imitate\\.([\\w_]+)", "mob.imitate.$1"))
            // firework has no prefix at all, and no _far sounds on bedrock
            .maybeTransform(Transformers.findAndReplace("mob\\.firework_rocket\\.([\\w_]+)", match -> "firework." + match.group(1).replace("_far", "")))
            // squids
            .maybeTransform(Transformers.findAndReplace("squirt", "ink_squirt"))
            // turtle eggs turn into blocks
            .maybeTransform(Transformers.findAndReplace("mob\\.turtle\\.egg_([\\w_]+)", "block.turtle_egg.$1"))
            // zombified_piglin.angry > zombiepig.zpigangry (yeah idk)
            .maybeTransform(Transformers.findAndReplace("mob\\.zombified_piglin\\.([\\w_]+)", match -> {
                String action = match.group(1);
                if (action.equals("ambient")) {
                    action = ""; // Ambient is just "zpig"
                }
                return "mob.zombiepig.zpig" + action;
            }))
            // _nautilus.ambient -> _nautilus.ambient.in.water
            .maybeTransform(Transformers.findAndReplace("mob\\.([\\w_]+_)?nautilus\\.([\\w_]+)", match -> {
                String type = match.group(1);
                String action = match.group(2);
                boolean land = action.endsWith("_land");
                if (land) {
                    action = action.replaceFirst("_land$", "");
                }
                return "mob." + (type == null ? "" : type) + "nautilus." + action + (land ? "" : ".in.water");
            }))
            // turtle.death_baby -> turtle_baby.death
            .maybeTransform(Transformers.findAndReplace("turtle\\.([\\w_]+)_baby", "turtle_baby.$1"))
            // goat.screaming.ambient -> goat.ambient.screamer
            .maybeTransform(Transformers.findAndReplace("goat\\.screaming\\.([\\w_]+)", "goat.$1.screamer"))

            // Find any exact matches
            .tryAll(Transformer.identity())
            // Try permutations for other things
            .tryAllPermutations(Transformer.Maybe::maybeChain,
                    // Common synonyms for idle/ambient/etc
                    Transformers.swap("idle", "ambient"),
                    Transformers.swap("fall", "land"),
                    Transformers.swap("hurt", "hit"),
                    Transformers.swap("step", "walk"),
                    Transformers.swap("riding", "ride"),
                    Transformers.swap("death", "kill"),
                    Transformers.findAndReplace("loop", "ambient"),
                    Transformers.findAndReplace("ambient", "say"),
                    Transformers.findAndReplace("ambient", "breathe"),
                    Transformers.findAndReplace("ambient_land", "land_idle"),
                    Transformers.findAndReplace("attack", "attack_loop"),
                    Transformers.findAndReplace("attack", "hurt"),
                    Transformers.findAndReplace("death_land", "land_death"),
                    Transformers.findAndReplace("hurt_land", "land_hit"),
                    Transformers.findAndReplace("swim", "step")
            )
            .reset() // End entity sound transformers

            .mark() // Start item sound transformers
            // First use ITEM_NAMES and ITEM_TYPES to map sound names and types
            .flatMapInput(Transformers.findAndReplaceGroups("item\\.([\\w_]+)\\.([\\w_.]+)", ITEM_NAMES.maybe(), ITEM_TYPES.maybe()))

            // Try without suffix
            .maybeTransform(Transformers.findAndReplace("^item\\.", ""))
            // item.wolf_armor.break -> armor.break_wolf
            .maybeTransform(Transformers.findAndReplace("item\\.wolf_armor\\.([\\w_]+)", "armor.$1_wolf"))
            // item.spear.lunge_1 -> item.enchant.lunge1
            .maybeTransform(Transformers.findAndReplace("item\\.spear\\.lunge_(\\d+)", "item.enchant.lunge$1"))
            // item.goat_horn.sound.1 -> horn.call.1
            .maybeTransform(Transformers.findAndReplace("item\\.goat_horn\\.sound\\.(\\d+)", "horn.call.$1"))
            // item.brush.brushing.generic -> brush.generic
            .maybeTransform(Transformers.findAndReplace("item\\.brush\\.brushing\\.([\\w_]+)", "brush.$1"))
            .maybeTransform(Transformers.findAndReplace("item\\.brush\\.brushing\\.(sand|gravel)", "brush.suspicious_$1"))
            .maybeTransform(Transformers.findAndReplace("item\\.brush\\.brushing\\.(sand|gravel)\\.complete", "brush_completed.suspicious_$1"))

            // Find any exact matches otherwise
            .tryAll(Transformer.identity())

            .reset() // End item sound transformers

            // Random other sound transformers

            .maybeTransform(Transformers.findAndReplace("ambient\\.soul_sand_valley", "ambient.soulsand_valley"))
            .maybeTransform(Transformers.findAndReplace("ambient\\.underwater\\.loop\\.additions[\\w_.]*", "ambient.underwater.additions"))
            .maybeTransform(Transformers.findAndReplace("event\\.mob_effect", "apply_effect"))
            .maybeTransform(Transformers.findAndReplace("music\\.[a-z_]+\\.", "music.game."))
            .maybeTransform(Transformers.findAndReplace("music_disc\\.", "record.")));

    // You've reached the end. Yay!

    private SoundMappings() {}

    private static Transformer.Maybe<String, String> category(String name) {
        return Transformers.findAndReplace("^" + name + "\\.", "");
    }

    private static Transformer.Maybe<String, String> filterForName(String name, String regex) {
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(name).matches() ? Transformer.Maybe.identity() : Transformer.Maybe.empty();
    }
}
