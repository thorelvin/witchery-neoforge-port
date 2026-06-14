package com.emoniph.witchery.events;

import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Passive "infusion identity" effects and ambient FX for an infused player.
 *
 * <p><b>Deliberate divergence from source:</b> 1.7.10 player infusions confer <em>no</em> passive
 * buffs at all — an infusion there is purely an energy pool plus the active powers channelled through
 * the Witch Hand (see {@code Infusion}/{@code ItemWitchHand}). The port keeps one small thematic effect
 * per infusion as its "infusion feel", documented in PORTING_NOTES / FEATURE_MATRIX. The real source
 * systems (energy bar, active powers, creature powers) land in later phases.
 */
@EventBusSubscriber
public class InfusionEvents {

    private static final int REFRESH_INTERVAL = 100; // ticks between buff refreshes
    private static final int EFFECT_DURATION   = 140; // slightly longer than the interval
    private static final int AURA_INTERVAL     = 12;  // ticks between ambient aura puffs

    private static final int INFERNAL_DARKNESS_DURATION = 100; // 5 s grant flash

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        InfusionType type = data.getInfusionType();
        if (type == InfusionType.NONE) return;

        if (player.tickCount % REFRESH_INTERVAL == 0) {
            applyIdentityEffect(player, type);
        }
        if (player.tickCount % AURA_INTERVAL == 0) {
            spawnAura(player, type);
        }
    }

    /**
     * The dramatic flash when an infusion is first granted (Rite of Infusion or the debug command).
     * Infernal also leaves a brief Darkness as a portentous flourish.
     */
    public static void onInfusionGranted(ServerPlayer player, InfusionType type) {
        if (type == InfusionType.NONE) return;
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(particleFor(type), player.getX(), player.getY() + 1.0, player.getZ(),
                48, 0.5, 0.9, 0.5, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.2F);
        if (type == InfusionType.INFERNAL) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, INFERNAL_DARKNESS_DURATION, 0, true, false));
        }
    }

    // ── One thematic effect per infusion (the port's "infusion identity") ───────

    private static void applyIdentityEffect(ServerPlayer player, InfusionType type) {
        switch (type) {
            // Ghost of the Light — illumination: the witch sees in the dark.
            case LIGHT -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, true, false));
            // Soul of the World — nature affinity: the living ground sustains her, but only while she stands on it.
            case OVERWORLD -> {
                if (isOnNaturalGround(player)) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION, 0, true, false));
                }
            }
            // Spirit of Otherwhere — the far country: she drifts rather than falls.
            case OTHERWHERE -> player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_DURATION, 0, true, false));
            // Infernal Animus — fire immunity: the flames know her as kin.
            case INFERNAL -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, true, false));
            case NONE -> { }
        }
    }

    private static boolean isOnNaturalGround(ServerPlayer player) {
        var below = player.level().getBlockState(player.blockPosition().below());
        return below.is(BlockTags.DIRT) || below.is(BlockTags.LEAVES)
                || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.FARMLAND);
    }

    // ── Ambient aura FX ─────────────────────────────────────────────────────────

    private static void spawnAura(ServerPlayer player, InfusionType type) {
        ((ServerLevel) player.level()).sendParticles(particleFor(type),
                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                2, 0.4, 0.6, 0.4, 0.0);
    }

    private static ParticleOptions particleFor(InfusionType type) {
        return switch (type) {
            case LIGHT      -> ParticleTypes.END_ROD;
            case OVERWORLD  -> ParticleTypes.HAPPY_VILLAGER;
            case OTHERWHERE -> ParticleTypes.PORTAL;
            case INFERNAL   -> ParticleTypes.SMALL_FLAME;
            case NONE       -> ParticleTypes.POOF;
        };
    }
}
