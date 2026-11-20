package org.geysermc.mappings.transformer.instance;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.geysermc.mappings.definitions.particle.ParticleColor;
import org.geysermc.mappings.definitions.particle.ParticleMapping;
import org.geysermc.mappings.transformer.Transformer;

import java.util.List;
import java.util.Objects;

public final class ParticleMappings {
    // From the top to the bottom:
    // - Simple direct identifier to identifier mappings, the best kind
    //  - Optional pack mappings
    //  - Mappings requiring a colour
    // - Cloudburst/ParticleType enum mappings, meh but still occur frequently
    // - Cloudburst/LevelEvent enum mappings, only a few
    // - Built-in handling in Geyser, for special particles, only a few
    // Also see Mojang's ParticleTypes class, we're following the same order
    public static final Transformer.MaybeCreator<List<Identifier>, ParticleType<?>, ParticleMapping> INSTANCE = validParticles -> Transformer.maybe(builder -> builder
            .check(mapping -> mapping.validate(validParticles))
            .mark()

            // Simple identifier particle mappings (the best kind)
            .mapOutput(ParticleMapping::ofIdentifier)

            .transform(ParticleTypes.ANGRY_VILLAGER, "villager_angry")
            .transform(ParticleTypes.BUBBLE, "basic_bubble_particle_manual")
            .transform(ParticleTypes.SULFUR_BUBBLES, "sulfur_bubble_particle")
            .transform(ParticleTypes.NOXIOUS_GAS, "noxious_gas_particle")
            .transform(ParticleTypes.CLOUD, "water_evaporation_bucket_emitter")
            .transform(ParticleTypes.COPPER_FIRE_FLAME, "green_flame_particle")
            .transform(ParticleTypes.CRIT, "critical_hit_emitter")
            .transform(ParticleTypes.DRAGON_BREATH, "dragon_breath_lingering")
            .transform(ParticleTypes.DRIPPING_LAVA, "lava_drip_particle")
            .transform(ParticleTypes.FALLING_LAVA, "lava_drip_particle")
            .transform(ParticleTypes.DRIPPING_WATER, "water_drip_particle")
            .transform(ParticleTypes.FALLING_WATER, "water_splash_particle")
            .transform(ParticleTypes.ENCHANT, "enchanting_table_particle")
            .transform(ParticleTypes.END_ROD, "endrod")
            .transform(ParticleTypes.EXPLOSION_EMITTER, "huge_explosion_emitter")
            .transform(ParticleTypes.EXPLOSION, "large_explosion")
            .transform(ParticleTypes.GUST, "wind_charged_emitter") // FIXME needs optional pack
            .transform(ParticleTypes.SMALL_GUST, "wind_charged_emitter")
            .transform(ParticleTypes.GUST_EMITTER_LARGE, "breeze_wind_explosion_emitter")
            .transform(ParticleTypes.GUST_EMITTER_SMALL, "wind_explosion_emitter")
            .transform(ParticleTypes.SONIC_BOOM, "sonic_explosion")
            .transform(ParticleTypes.FISHING, "water_wake_particle")
            .transform(ParticleTypes.FLAME, "basic_flame_particle")
            .transform(ParticleTypes.INFESTED, "infested_emitter")
            .transform(ParticleTypes.CHERRY_LEAVES, "cherry_leaves_particle")
            .transform(ParticleTypes.PALE_OAK_LEAVES, "pale_oak_leaves_particle")
            .transform(ParticleTypes.RED_POPLAR_LEAVES, "red_poplar_leaves_particle")
            .transform(ParticleTypes.ORANGE_POPLAR_LEAVES, "orange_poplar_leaves_particle")
            .transform(ParticleTypes.YELLOW_POPLAR_LEAVES, "yellow_poplar_leaves_particle")
            .transform(ParticleTypes.SCULK_SOUL, "sculk_soul_particle")
            .transform(ParticleTypes.SOUL_FIRE_FLAME, "blue_flame_particle")
            .transform(ParticleTypes.SOUL, "soul_particle")
            .transform(ParticleTypes.HAPPY_VILLAGER, "villager_happy")
            .transform(ParticleTypes.COMPOSTER, "villager_happy")
            .transform(ParticleTypes.HEART, "heart_particle")
            .transform(ParticleTypes.LARGE_SMOKE, "water_evaporation_actor_emitter")
            .transform(ParticleTypes.LAVA, "lava_particle")
            .transform(ParticleTypes.MYCELIUM, "mycelium_dust_particle")
            .transform(ParticleTypes.POOF, "explosion_manual")
            .transform(ParticleTypes.PORTAL, "mob_portal")
            .transform(ParticleTypes.RAIN, "rain_splash_particle")
            .transform(ParticleTypes.SMOKE, "basic_smoke_particle")
            .transform(ParticleTypes.WHITE_SMOKE, "white_smoke_particle")
            .transform(ParticleTypes.SPIT, "llama_spit_smoke")
            .transform(ParticleTypes.SQUID_INK, "squid_ink_bubble")
            .transform(ParticleTypes.TOTEM_OF_UNDYING, "totem_particle")
            .transform(ParticleTypes.SPLASH, "water_splash_particle_manual")
            .transform(ParticleTypes.WITCH, "witchspell_emitter")
            .transform(ParticleTypes.BUBBLE_POP, "bubble_column_bubble")
            .transform(ParticleTypes.CURRENT_DOWN, "bubble_column_down_particle")
            .transform(ParticleTypes.BUBBLE_COLUMN_UP, "bubble_column_up_particle")
            .transform(ParticleTypes.DOLPHIN, "dolphin_move_particle")
            .transform(ParticleTypes.CAMPFIRE_COSY_SMOKE, "campfire_smoke_particle")
            .transform(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, "campfire_tall_smoke_particle")
            .transform(ParticleTypes.DRIPPING_HONEY, "honey_drip_particle")
            .transform(ParticleTypes.FALLING_HONEY, "honey_drip_particle")
            .transform(ParticleTypes.FALLING_NECTAR, "nectar_drip_particle")
            .transform(ParticleTypes.FALLING_SPORE_BLOSSOM, "spore_blossom_shower_particle")
            .transform(ParticleTypes.SPORE_BLOSSOM_AIR, "spore_blossom_ambient_particle")
            .transform(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, "obsidian_tear_particle")
            .transform(ParticleTypes.FALLING_OBSIDIAN_TEAR, "obsidian_tear_particle")
            .transform(ParticleTypes.REVERSE_PORTAL, "portal_reverse_particle")
            .transform(ParticleTypes.SMALL_FLAME, "candle_flame_particle")
            .transform(ParticleTypes.SNOWFLAKE, "snowflake_particle")
            .transform(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, "stalactite_lava_drip_particle")
            .transform(ParticleTypes.FALLING_DRIPSTONE_LAVA, "stalactite_lava_drip_particle")
            .transform(ParticleTypes.DRIPPING_DRIPSTONE_WATER, "stalactite_water_drip_particle")
            .transform(ParticleTypes.FALLING_DRIPSTONE_WATER, "stalactite_water_drip_particle")
            .transform(ParticleTypes.GLOW_SQUID_INK, "squid_ink_bubble")
            .transform(ParticleTypes.ELECTRIC_SPARK, "electric_spark_particle")
            .transform(ParticleTypes.SHRIEK, "shriek_particle")
            .transform(ParticleTypes.EGG_CRACK, "villager_happy")
            .transform(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, "trial_spawner_detection")
            .transform(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, "trial_spawner_detection_ominous")
            .transform(ParticleTypes.VAULT_CONNECTION, "vault_connection_particle")
            .transform(ParticleTypes.DUST_PILLAR, "smash_ground_particle")
            .transform(ParticleTypes.OMINOUS_SPAWNING, "ominous_spawning_particle")
            .transform(ParticleTypes.RAID_OMEN, "raid_omen_ambient")
            .transform(ParticleTypes.TRIAL_OMEN, "trial_omen_emitter")
            .transform(ParticleTypes.BLOCK_CRUMBLE, "creaking_crumble_body")
            .transform(ParticleTypes.FIREFLY, "firefly_particle")

            // Implemented in Geyser's resourcepack
            .transform(ParticleTypes.DAMAGE_INDICATOR, "geyseropt:damage_indicator")
            .transform(ParticleTypes.LANDING_LAVA, "geyseropt:landing_lava")
            .transform(ParticleTypes.ENCHANTED_HIT, "geyseropt:enchanted_hit_single")
            .transform(ParticleTypes.FLASH, "geyseropt:flash")
            .transform(ParticleTypes.SWEEP_ATTACK, "geyseropt:sweep_attack")
            .transform(ParticleTypes.UNDERWATER, "geyseropt:underwater")
            .transform(ParticleTypes.NAUTILUS, "geyseropt:nautilus")
            .transform(ParticleTypes.LANDING_HONEY, "geyseropt:landing_honey")
            .transform(ParticleTypes.ASH, "geyseropt:ash")
            .transform(ParticleTypes.CRIMSON_SPORE, "geyseropt:crimson_spore")
            .transform(ParticleTypes.WARPED_SPORE, "geyseropt:warped_spore")
            .transform(ParticleTypes.LANDING_OBSIDIAN_TEAR, "geyseropt:landing_obsidian_tear")
            .transform(ParticleTypes.WHITE_ASH, "geyseropt:white_ash")

            // Identifier particle mappings requiring one or more colours
            .reset()
            // Always white on Java Edition, requires colour on bedrock
            .transform(ParticleTypes.FIREWORK, ParticleMapping.ofColored("sparkler_emitter", 0xFFFFFF))
            // Always random on Java Edition, requires colour on bedrock (side note: thanks for the one variable edge case, Mojang)
            .transform(ParticleTypes.NOTE, ParticleMapping.ofColored("note_particle", ParticleColor.random("variable.note_color")))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L188
            .transform(ParticleTypes.WAX_ON, ParticleMapping.ofColored("wax_particle", 0xE88C14))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L159
            .transform(ParticleTypes.WAX_OFF, ParticleMapping.ofColored("wax_particle", 0xFFE5FF))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L88-92
            .transform(ParticleTypes.GLOW, ParticleMapping.ofColored("glow_particle", ParticleColor.random(0x99FFCC, 0x146666)))
            // See https://mcsrc.dev/2/26.3/net/minecraft/client/particle/GlowParticle#L125-129
            .transform(ParticleTypes.SCRAPE, ParticleMapping.ofColored("wax_particle", ParticleColor.random(0x499382, 0x6DC49E)))

            // Cloudburst ParticleType enum mappings (a little ugly)
            .reset()
            .mapOutput(ParticleMapping::ofType)

            .transform(ParticleTypes.ITEM_SLIME, org.cloudburstmc.protocol.bedrock.data.ParticleType.SLIME)
            .transform(ParticleTypes.ITEM_SNOWBALL, org.cloudburstmc.protocol.bedrock.data.ParticleType.SNOWBALL_POOF)

            // Cloudburst LevelEvent enum mappings (booo)
            .reset()
            .mapOutput(ParticleMapping::ofEvent)

            .transform(ParticleTypes.ELDER_GUARDIAN, LevelEvent.PARTICLE_SOUND_GUARDIAN_GHOST)
            .transform(ParticleTypes.SCULK_CHARGE, LevelEvent.SCULK_CHARGE)
            .transform(ParticleTypes.SCULK_CHARGE_POP, LevelEvent.SCULK_CHARGE_POP)
            .transform(ParticleTypes.ITEM_COBWEB, LevelEvent.ANIMATION_SPAWN_COBWEB)

            // Built-in mappings (handled in Geyser, hardcoded)
            .reset()

            .transform(ParticleTypes.BLOCK, ParticleMapping.builtIn())
            .transform(ParticleTypes.GEYSER, ParticleMapping.builtIn())
            .transform(ParticleTypes.GEYSER_BASE, ParticleMapping.builtIn())
            .transform(ParticleTypes.GEYSER_PLUME, ParticleMapping.builtIn())
            .transform(ParticleTypes.GEYSER_POOF, ParticleMapping.builtIn())
            .transform(ParticleTypes.DUST, ParticleMapping.builtIn())
            .transform(ParticleTypes.DUST_COLOR_TRANSITION, ParticleMapping.builtIn())
            .transform(ParticleTypes.EFFECT, ParticleMapping.builtIn())
            .transform(ParticleTypes.FALLING_DUST, ParticleMapping.builtIn())
            .transform(ParticleTypes.TINTED_LEAVES, ParticleMapping.builtIn())
            .transform(ParticleTypes.INSTANT_EFFECT, ParticleMapping.builtIn())
            .transform(ParticleTypes.ITEM, ParticleMapping.builtIn())
            .transform(ParticleTypes.VIBRATION, ParticleMapping.builtIn())
            .transform(ParticleTypes.TRAIL, ParticleMapping.builtIn())

            .reset()
            // Try to map the remaining particles - if an identical name exists it'll be used, if not the validator will fail it
            .catchAll(type -> ParticleMapping.ofIdentifier(Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type)).getPath()))
    );
}
