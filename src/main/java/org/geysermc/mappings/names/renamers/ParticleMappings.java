package org.geysermc.mappings.names.renamers;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.geysermc.mappings.definitions.particle.ParticleColor;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.names.TypeRenamer;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class ParticleMappings {
    // From the top to the bottom:
    // - Simple direct identifier to identifier mappings, the best kind
    //  - Optional pack mappings
    //  - Mappings requiring a colour
    // - Cloudburst/ParticleType enum mappings, meh but still occur frequently
    // - Cloudburst/LevelEvent enum mappings, only a few
    // - Built-in handling in Geyser, for special particles, only a few
    // Also see Mojang's ParticleTypes class, we're following the same order
    public static final TypeRenamer<ParticleType<?>, Optional<ParticleMapping>> PARTICLES = TypeRenamer.ofOptional(builder -> builder
            // Simple identifier particle mappings (the best kind)
            .mapResult(ParticleMapping::ofIdentifier)
            .withIdenticalNamer(type -> Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type)).getPath())

            .rename(ParticleTypes.ANGRY_VILLAGER, "villager_angry")
            .rename(ParticleTypes.BUBBLE, "basic_bubble_particle_manual")
            .rename(ParticleTypes.SULFUR_BUBBLES, "sulfur_bubble_particle")
            .rename(ParticleTypes.NOXIOUS_GAS, "noxious_gas_particle")
            .rename(ParticleTypes.CLOUD, "water_evaporation_bucket_emitter")
            .rename(ParticleTypes.COPPER_FIRE_FLAME, "green_flame_particle")
            .rename(ParticleTypes.CRIT, "critical_hit_emitter")
            .rename(ParticleTypes.DRAGON_BREATH, "dragon_breath_lingering")
            .rename(ParticleTypes.DRIPPING_LAVA, "lava_drip_particle")
            .rename(ParticleTypes.FALLING_LAVA, "lava_drip_particle")
            .rename(ParticleTypes.DRIPPING_WATER, "water_drip_particle")
            .rename(ParticleTypes.FALLING_WATER, "water_splash_particle")
            .rename(ParticleTypes.ENCHANT, "enchanting_table_particle")
            .rename(ParticleTypes.END_ROD, "endrod")
            .rename(ParticleTypes.EXPLOSION_EMITTER, "huge_explosion_emitter")
            .rename(ParticleTypes.EXPLOSION, "large_explosion")
            .rename(ParticleTypes.GUST, "wind_charged_emitter") // FIXME needs optional pack
            .rename(ParticleTypes.SMALL_GUST, "wind_charged_emitter")
            .rename(ParticleTypes.GUST_EMITTER_LARGE, "breeze_wind_explosion_emitter")
            .rename(ParticleTypes.GUST_EMITTER_SMALL, "wind_explosion_emitter")
            .rename(ParticleTypes.SONIC_BOOM, "sonic_explosion")
            .rename(ParticleTypes.FISHING, "water_wake_particle")
            .rename(ParticleTypes.FLAME, "basic_flame_particle")
            .rename(ParticleTypes.INFESTED, "infested_emitter")
            .rename(ParticleTypes.CHERRY_LEAVES, "cherry_leaves_particle")
            .rename(ParticleTypes.PALE_OAK_LEAVES, "pale_oak_leaves_particle")
            .rename(ParticleTypes.RED_POPLAR_LEAVES, "red_poplar_leaves_particle")
            .rename(ParticleTypes.ORANGE_POPLAR_LEAVES, "orange_poplar_leaves_particle")
            .rename(ParticleTypes.YELLOW_POPLAR_LEAVES, "yellow_poplar_leaves_particle")
            .rename(ParticleTypes.SCULK_SOUL, "sculk_soul_particle")
            .rename(ParticleTypes.SOUL_FIRE_FLAME, "blue_flame_particle")
            .rename(ParticleTypes.SOUL, "soul_particle")
            .rename(ParticleTypes.HAPPY_VILLAGER, "villager_happy")
            .rename(ParticleTypes.COMPOSTER, "villager_happy")
            .rename(ParticleTypes.HEART, "heart_particle")
            .identical(ParticleTypes.PAUSE_MOB_GROWTH)
            .identical(ParticleTypes.RESET_MOB_GROWTH)
            .rename(ParticleTypes.LARGE_SMOKE, "water_evaporation_actor_emitter")
            .rename(ParticleTypes.LAVA, "lava_particle")
            .rename(ParticleTypes.MYCELIUM, "mycelium_dust_particle")
            .rename(ParticleTypes.POOF, "explosion_manual")
            .rename(ParticleTypes.PORTAL, "mob_portal")
            .rename(ParticleTypes.RAIN, "rain_splash_particle")
            .rename(ParticleTypes.SMOKE, "basic_smoke_particle")
            .rename(ParticleTypes.WHITE_SMOKE, "white_smoke_particle")
            .identical(ParticleTypes.SNEEZE)
            .rename(ParticleTypes.SPIT, "llama_spit_smoke")
            .rename(ParticleTypes.SQUID_INK, "squid_ink_bubble")
            .rename(ParticleTypes.TOTEM_OF_UNDYING, "totem_particle")
            .rename(ParticleTypes.SPLASH, "water_splash_particle_manual")
            .rename(ParticleTypes.WITCH, "witchspell_emitter")
            .rename(ParticleTypes.BUBBLE_POP, "bubble_column_bubble")
            .rename(ParticleTypes.CURRENT_DOWN, "bubble_column_down_particle")
            .rename(ParticleTypes.BUBBLE_COLUMN_UP, "bubble_column_up_particle")
            .rename(ParticleTypes.DOLPHIN, "dolphin_move_particle")
            .rename(ParticleTypes.CAMPFIRE_COSY_SMOKE, "campfire_smoke_particle")
            .rename(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, "campfire_tall_smoke_particle")
            .rename(ParticleTypes.DRIPPING_HONEY, "honey_drip_particle")
            .rename(ParticleTypes.FALLING_HONEY, "honey_drip_particle")
            .rename(ParticleTypes.FALLING_NECTAR, "nectar_drip_particle")
            .rename(ParticleTypes.FALLING_SPORE_BLOSSOM, "spore_blossom_shower_particle")
            .rename(ParticleTypes.SPORE_BLOSSOM_AIR, "spore_blossom_ambient_particle")
            .rename(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, "obsidian_tear_particle")
            .rename(ParticleTypes.FALLING_OBSIDIAN_TEAR, "obsidian_tear_particle")
            .rename(ParticleTypes.REVERSE_PORTAL, "portal_reverse_particle")
            .rename(ParticleTypes.SMALL_FLAME, "candle_flame_particle")
            .rename(ParticleTypes.SNOWFLAKE, "snowflake_particle")
            .rename(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, "stalactite_lava_drip_particle")
            .rename(ParticleTypes.FALLING_DRIPSTONE_LAVA, "stalactite_lava_drip_particle")
            .rename(ParticleTypes.DRIPPING_DRIPSTONE_WATER, "stalactite_water_drip_particle")
            .rename(ParticleTypes.FALLING_DRIPSTONE_WATER, "stalactite_water_drip_particle")
            .rename(ParticleTypes.GLOW_SQUID_INK, "squid_ink_bubble")
            .rename(ParticleTypes.ELECTRIC_SPARK, "electric_spark_particle")
            .rename(ParticleTypes.SHRIEK, "shriek_particle")
            .rename(ParticleTypes.EGG_CRACK, "villager_happy")
            .identical(ParticleTypes.DUST_PLUME)
            .rename(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, "trial_spawner_detection")
            .rename(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, "trial_spawner_detection_ominous")
            .rename(ParticleTypes.VAULT_CONNECTION, "vault_connection_particle")
            .rename(ParticleTypes.DUST_PILLAR, "smash_ground_particle")
            .rename(ParticleTypes.OMINOUS_SPAWNING, "ominous_spawning_particle")
            .rename(ParticleTypes.RAID_OMEN, "raid_omen_ambient")
            .rename(ParticleTypes.TRIAL_OMEN, "trial_omen_emitter")
            .rename(ParticleTypes.BLOCK_CRUMBLE, "creaking_crumble_body")
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
            .rename(ParticleTypes.LANDING_HONEY, "geyseropt:landing_honey")
            .rename(ParticleTypes.ASH, "geyseropt:ash")
            .rename(ParticleTypes.CRIMSON_SPORE, "geyseropt:crimson_spore")
            .rename(ParticleTypes.WARPED_SPORE, "geyseropt:warped_spore")
            .rename(ParticleTypes.LANDING_OBSIDIAN_TEAR, "geyseropt:landing_obsidian_tear")
            .rename(ParticleTypes.WHITE_ASH, "geyseropt:white_ash")

            // Identifier particle mappings requiring one or more colours
            .mapResult(Function.identity())
            // Always white on Java Edition, requires colour on bedrock
            .rename(ParticleTypes.FIREWORK, ParticleMapping.ofColored("sparkler_emitter", 0xFFFFFF))
            // Always random on Java Edition, requires colour on bedrock
            .rename(ParticleTypes.NOTE, ParticleMapping.ofColored("note_particle", ParticleColor.random()))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L188
            .rename(ParticleTypes.WAX_ON, ParticleMapping.ofColored("wax_particle", 0xE88C14))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L159
            .rename(ParticleTypes.WAX_OFF, ParticleMapping.ofColored("wax_particle", 0xFFE5FF))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L88-92
            .rename(ParticleTypes.GLOW, ParticleMapping.ofColored("glow_particle", ParticleColor.random(0x99FFCC, 0x146666)))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L125-129
            .rename(ParticleTypes.SCRAPE, ParticleMapping.ofColored("wax_particle", ParticleColor.random(0x499382, 0x6DC49E)))

            // Cloudburst ParticleType enum mappings (a little ugly)
            .mapResult(ParticleMapping::ofType)
            .withIdenticalNamer(type -> org.cloudburstmc.protocol.bedrock.data.ParticleType.valueOf(Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type)).getPath().toUpperCase(Locale.ROOT)))

            .rename(ParticleTypes.ITEM_SLIME, org.cloudburstmc.protocol.bedrock.data.ParticleType.SLIME)
            .rename(ParticleTypes.ITEM_SNOWBALL, org.cloudburstmc.protocol.bedrock.data.ParticleType.SNOWBALL_POOF)

            // Cloudburst LevelEvent enum mappings (booo)
            .mapResult(ParticleMapping::ofEvent)

            .rename(ParticleTypes.ELDER_GUARDIAN, LevelEvent.PARTICLE_SOUND_GUARDIAN_GHOST)
            .rename(ParticleTypes.SCULK_CHARGE, LevelEvent.SCULK_CHARGE)
            .rename(ParticleTypes.SCULK_CHARGE_POP, LevelEvent.SCULK_CHARGE_POP)
            .rename(ParticleTypes.ITEM_COBWEB, LevelEvent.ANIMATION_SPAWN_COBWEB)

            // Built-in mappings (handled in Geyser, hardcoded)
            // Excuse the method names, they seem a bit odd, this is abusing the renamer stuff a little
            .mapResult(Function.identity())
            .withIdenticalNamer(_ -> ParticleMapping.builtIn())

            .identical(ParticleTypes.BLOCK)
            .identical(ParticleTypes.GEYSER)
            .identical(ParticleTypes.GEYSER_BASE)
            .identical(ParticleTypes.GEYSER_PLUME)
            .identical(ParticleTypes.GEYSER_POOF)
            .identical(ParticleTypes.DUST)
            .identical(ParticleTypes.DUST_COLOR_TRANSITION)
            .identical(ParticleTypes.EFFECT)
            .identical(ParticleTypes.FALLING_DUST)
            .identical(ParticleTypes.TINTED_LEAVES)
            .identical(ParticleTypes.INSTANT_EFFECT)
            .identical(ParticleTypes.ITEM)
            .identical(ParticleTypes.VIBRATION)
            .identical(ParticleTypes.TRAIL)
    );
}
