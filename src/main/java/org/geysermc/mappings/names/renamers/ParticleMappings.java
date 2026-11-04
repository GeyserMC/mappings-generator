package org.geysermc.mappings.names.renamers;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.names.TypeRenamer;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

// block, block_marker, noxious_gas_cloud, geyser, dust, dust_color_transition, effect, falling_dust, firework, tinted_leaves, item,
// vibration, trail, wax_on, wax_off, glow, scrape
// level events: elder_guardian, pale_oak_leaves, poplar_leaves, sculk_charge, sculk_charge_pop, mob_growth, item_slime, item_cobweb, item_snowball, rain, smoke, white_smoke,
// sneeze, squid_ink, witch, dripping_honey, dripping_obsidian_tear, falling_obsidian_tear, snowflake, glow_squid_ink, electric_spark,
// vault_connection, block_crumble,
public final class ParticleMappings {
    // See Mojang's ParticleTypes class, we're following the same order - except for geyseropt particles,
    // those are at the bottom (also in the same order)
    // Also see the particles needing special handling in Geyser (noted in ParticleMappingsGenerator)
    public static final TypeRenamer<ParticleType<?>, Optional<ParticleMapping>> PARTICLES = TypeRenamer.ofOptional(builder -> builder
            // Simple identifier particle mappings (the best kind)
            .mapResult(ParticleMapping::ofIdentifier)
            .withIdenticalNamer(type -> Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type)).getPath())

            .rename(ParticleTypes.ANGRY_VILLAGER, "villager_angry")
            .rename(ParticleTypes.BUBBLE, "basic_bubble_particle_manual")
            .rename(ParticleTypes.SULFUR_BUBBLES, "sulfur_bubble_particle")
            .rename(ParticleTypes.NOXIOUS_GAS, "noxious_gas_particle")
            .identical(ParticleTypes.GEYSER_BASE)
            .identical(ParticleTypes.GEYSER_PLUME)
            .identical(ParticleTypes.GEYSER_POOF)
            .rename(ParticleTypes.CLOUD, "water_evaporation_bucket_emitter")
            .rename(ParticleTypes.COPPER_FIRE_FLAME, "green_flame_particle")
            .rename(ParticleTypes.CRIT, "critical_hit_emitter")
            .rename(ParticleTypes.DRAGON_BREATH, "dragon_breath_lingering")
            .rename(ParticleTypes.DRIPPING_LAVA, "lava_drip_particle")
            .rename(ParticleTypes.FALLING_LAVA, "lava_drip_particle") // TODO is this right?
            .rename(ParticleTypes.DRIPPING_WATER, "water_drip_particle")
            .rename(ParticleTypes.FALLING_WATER, "water_splash_particle")
            .rename(ParticleTypes.ENCHANT, "enchanting_table_particle")
            .rename(ParticleTypes.END_ROD, "endrod")
            .rename(ParticleTypes.ENTITY_EFFECT, "evoker_spell") // TODO is this right?
            .rename(ParticleTypes.EXPLOSION_EMITTER, "huge_explosion_emitter")
            .rename(ParticleTypes.EXPLOSION, "large_explosion")
            .rename(ParticleTypes.GUST, "breeze_wind_explosion_emitter")
            .rename(ParticleTypes.SMALL_GUST, "wind_charged_emitter")
            .rename(ParticleTypes.GUST_EMITTER_LARGE, "wind_explosion_emitter")
            .rename(ParticleTypes.GUST_EMITTER_SMALL, "breeze_wind_explosion_emitter")  // TODO ^ pretty sure these 2 are wrong way round
            .rename(ParticleTypes.SONIC_BOOM, "sonic_explosion")
            .rename(ParticleTypes.FISHING, "water_wake_particle")
            .rename(ParticleTypes.FLAME, "basic_flame_particle")
            .rename(ParticleTypes.INFESTED, "infested_emitter")
            .rename(ParticleTypes.CHERRY_LEAVES, "cherry_leaves_particle")
            .rename(ParticleTypes.SCULK_SOUL, "sculk_soul_particle")
            .rename(ParticleTypes.SOUL_FIRE_FLAME, "blue_flame_particle")
            .rename(ParticleTypes.SOUL, "soul_particle")
            .rename(ParticleTypes.HAPPY_VILLAGER, "villager_happy")
            .rename(ParticleTypes.COMPOSTER, "villager_happy")
            .rename(ParticleTypes.HEART, "heart_particle")
            .rename(ParticleTypes.INSTANT_EFFECT, "mobspell_emitter") // TODO is this right?
            .rename(ParticleTypes.LARGE_SMOKE, "water_evaporation_actor_emitter")
            .rename(ParticleTypes.LAVA, "lava_particle")
            .rename(ParticleTypes.MYCELIUM, "mycelium_dust_particle")
            .rename(ParticleTypes.NOTE, "note_particle")
            .rename(ParticleTypes.POOF, "explosion_manual")
            .rename(ParticleTypes.PORTAL, "mob_portal")
            .rename(ParticleTypes.SPIT, "llama_spit_smoke")
            .rename(ParticleTypes.TOTEM_OF_UNDYING, "totem_particle")
            .rename(ParticleTypes.SPLASH, "water_splash_particle_manual")
            .rename(ParticleTypes.BUBBLE_POP, "bubble_column_bubble") // TODO is this right?
            .rename(ParticleTypes.CURRENT_DOWN, "bubble_column_down_particle")
            .rename(ParticleTypes.BUBBLE_COLUMN_UP, "bubble_column_up_particle")
            .rename(ParticleTypes.DOLPHIN, "dolphin_move_particle")
            .rename(ParticleTypes.CAMPFIRE_COSY_SMOKE, "campfire_smoke_particle")
            .rename(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, "campfire_tall_smoke_particle") // TODO test movement without level type
            .rename(ParticleTypes.FALLING_HONEY, "honey_drip_particle") // TODO is this right?
            .rename(ParticleTypes.FALLING_NECTAR, "nectar_drip_particle")
            .rename(ParticleTypes.FALLING_SPORE_BLOSSOM, "spore_blossom_shower_particle")
            .rename(ParticleTypes.SPORE_BLOSSOM_AIR, "spore_blossom_ambient_particle")
            .rename(ParticleTypes.REVERSE_PORTAL, "portal_reverse_particle")
            .rename(ParticleTypes.SMALL_FLAME, "candle_flame_particle")
            .rename(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, "stalactite_lava_drip_particle")
            .rename(ParticleTypes.FALLING_DRIPSTONE_LAVA, "stalactite_lava_drip_particle")
            .rename(ParticleTypes.DRIPPING_DRIPSTONE_WATER, "stalactite_water_drip_particle")
            .rename(ParticleTypes.FALLING_DRIPSTONE_WATER, "stalactite_water_drip_particle")
            .rename(ParticleTypes.SHRIEK, "shriek_particle")
            .rename(ParticleTypes.EGG_CRACK, "villager_happy")
            .identical(ParticleTypes.DUST_PLUME)
            .rename(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, "trial_spawner_detection")
            .rename(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, "trial_spawner_detection_ominous")
            .rename(ParticleTypes.DUST_PILLAR, "smash_ground_particle")
            .rename(ParticleTypes.OMINOUS_SPAWNING, "ominous_spawning_particle")
            .rename(ParticleTypes.RAID_OMEN, "raid_omen_ambient")
            .rename(ParticleTypes.TRIAL_OMEN, "trial_omen_emitter") // TODO ^ should they both be emitter?
            .rename(ParticleTypes.FIREFLY, "firefly_particle")
            .identical(ParticleTypes.SULFUR_CUBE_GOO)

            // Implemented in Geyser's resourcepack
            .rename(ParticleTypes.DAMAGE_INDICATOR, "geyseropt:damage_indicator")
            .rename(ParticleTypes.LANDING_LAVA, "geyseropt:landing_lava")
            .rename(ParticleTypes.ENCHANTED_HIT, "geyseropt:enchanted_hit_single")
            .rename(ParticleTypes.FLASH, "geyseropt:flash")
            .rename(ParticleTypes.SWEEP_ATTACK, "geyseropt:sweep_attack")
            .rename(ParticleTypes.UNDERWATER, "geyseropt:underwater")
            .rename(ParticleTypes.NAUTILUS, "geyseropt:nautilus")
            .rename(ParticleTypes.ASH, "geyseropt:ash")
            .rename(ParticleTypes.CRIMSON_SPORE, "geyseropt:crimson_spore")
            .rename(ParticleTypes.WARPED_SPORE, "geyseropt:warped_spore")
            .rename(ParticleTypes.LANDING_OBSIDIAN_TEAR, "geyseropt:landing_obsidian_tear")
            .rename(ParticleTypes.WHITE_ASH, "geyseropt:white_ash")

            // Cloudburst ParticleType enum mappings (a little ugly)
            .mapResult(ParticleMapping::ofType)
            .withIdenticalNamer(type -> org.cloudburstmc.protocol.bedrock.data.ParticleType.valueOf(Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type)).getPath().toUpperCase(Locale.ROOT)))

            .identical(ParticleTypes.PALE_OAK_LEAVES)
            .identical(ParticleTypes.RED_POPLAR_LEAVES)
            .identical(ParticleTypes.ORANGE_POPLAR_LEAVES)
            .identical(ParticleTypes.YELLOW_POPLAR_LEAVES)
            .identical(ParticleTypes.PAUSE_MOB_GROWTH)
            .identical(ParticleTypes.RESET_MOB_GROWTH)
            .rename(ParticleTypes.ITEM_SLIME, org.cloudburstmc.protocol.bedrock.data.ParticleType.SLIME)
            .rename(ParticleTypes.ITEM_SNOWBALL, org.cloudburstmc.protocol.bedrock.data.ParticleType.SNOWBALL_POOF)
            .rename(ParticleTypes.RAIN, org.cloudburstmc.protocol.bedrock.data.ParticleType.RAIN_SPLASH)
            .identical(ParticleTypes.SMOKE)
            .identical(ParticleTypes.WHITE_SMOKE)
            .identical(ParticleTypes.SNEEZE)
            .rename(ParticleTypes.SQUID_INK, org.cloudburstmc.protocol.bedrock.data.ParticleType.INK)
            .rename(ParticleTypes.WITCH, org.cloudburstmc.protocol.bedrock.data.ParticleType.WITCH_SPELL)
            .rename(ParticleTypes.DRIPPING_HONEY, org.cloudburstmc.protocol.bedrock.data.ParticleType.DRIP_HONEY)
            .rename(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, org.cloudburstmc.protocol.bedrock.data.ParticleType.OBSIDIAN_TEAR)
            .rename(ParticleTypes.FALLING_OBSIDIAN_TEAR, org.cloudburstmc.protocol.bedrock.data.ParticleType.OBSIDIAN_TEAR)
            .identical(ParticleTypes.SNOWFLAKE)
            .rename(ParticleTypes.GLOW_SQUID_INK, org.cloudburstmc.protocol.bedrock.data.ParticleType.INK) // FIXME ?
            .identical(ParticleTypes.ELECTRIC_SPARK)
            .identical(ParticleTypes.VAULT_CONNECTION)
            .rename(ParticleTypes.BLOCK_CRUMBLE, org.cloudburstmc.protocol.bedrock.data.ParticleType.CREAKING_CRUMBLE)

            // Cloudburst LevelEvent enum mappings (booo)
            .mapResult(ParticleMapping::ofEvent)

            .rename(ParticleTypes.ELDER_GUARDIAN, LevelEvent.PARTICLE_SOUND_GUARDIAN_GHOST)
            .rename(ParticleTypes.SCULK_CHARGE, LevelEvent.SCULK_CHARGE)
            .rename(ParticleTypes.SCULK_CHARGE_POP, LevelEvent.SCULK_CHARGE_POP)
            .rename(ParticleTypes.ITEM_COBWEB, LevelEvent.ANIMATION_SPAWN_COBWEB)
    );
}
