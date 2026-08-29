package com.emoniph.witchery.events;

import com.emoniph.witchery.data.CreaturePowerType;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Source-backed infusion reactions plus non-mechanical grant and ambient presentation. */
@EventBusSubscriber
public class InfusionEvents {

    private static final int AURA_INTERVAL     = 12;  // ticks between ambient aura puffs

    private static final int INFERNAL_DARKNESS_DURATION = 100; // 5 s grant flash

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        InfusionType type = data.getInfusionType();
        if (type == InfusionType.NONE) return;

        if (player.tickCount % AURA_INTERVAL == 0) {
            spawnAura(player, type);
        }
    }

    /**
     * Overworld fall-cushion (source {@code InfusionOverworld.onFalling}): a hard landing on natural
     * ground spends infusion energy to negate the fall — softly, kicking the sod loose, or, while
     * sneaking, as a ground-slam explosion.
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDistance() <= 3.0f) {
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        switch (data.getInfusionType()) {
            case OVERWORLD -> overworldFallCushion(player, data, event);
            case INFERNAL -> creaturePowerFall(player, data, event);
            default -> { }
        }
    }

    private static void overworldFallCushion(ServerPlayer player, WitcheryPlayerData data, LivingFallEvent event) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos ground = player.blockPosition().below();
        if (!isOverworldFallGround(level.getBlockState(ground))) {
            return;
        }
        boolean slam = player.isShiftKeyDown();
        if (!spendEnergy(player, data, slam ? 10 : 5)) {
            return;
        }
        event.setDistance(0.0f);
        if (slam) {
            level.explode(player, player.getX(), ground.getY() + 0.5, player.getZ(), 3.0F,
                    Level.ExplosionInteraction.MOB);
        } else {
            level.destroyBlock(ground, true);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), ground.getY() + 1.0, player.getZ(),
                    16, 0.5, 0.1, 0.5, 0.0);
        }
    }

    /** Source creature-power onFalling (Infernal/beast infusion): Bat cushions, Jump negates a fall. */
    private static void creaturePowerFall(ServerPlayer player, WitcheryPlayerData data, LivingFallEvent event) {
        CreaturePowerType power = CreaturePowerType.fromString(data.getCreaturePowerType());
        if (power == CreaturePowerType.BAT) {
            if (event.getDistance() > 5.0f) {
                event.setDistance(5.0f);
            }
        } else if (power == CreaturePowerType.JUMP) {
            event.setDistance(0.0f);
            ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH,
                    SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    /**
     * Source creature-power {@code onDamage} reactions (Infernal/beast infusion). Each held power
     * spends infusion energy (or, for Creeper, gains charges) to ward off a thematic damage type:
     * Pigman/Blaze/Ghast extinguish fire, Skeleton/Squid breathe water under a drowning blow, and a
     * Creeper soaks up a lightning strike to recharge instead of being hurt.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getInfusionType() != InfusionType.INFERNAL) {
            return;
        }
        CreaturePowerType power = CreaturePowerType.fromString(data.getCreaturePowerType());
        if (power == null) {
            return;
        }
        switch (power) {
            // Source PigMan keeps fire-res for 1200t and reapplies even if already held; Blaze/Ghast
            // are the shorter 200t variant that no-ops while fire-res is already active.
            case PIGMAN -> wardFire(player, data, event, 1200, false);
            case BLAZE, GHAST -> wardFire(player, data, event, 200, true);
            case SKELETON -> wardDrown(player, data, event, 5, 0, 600, 30);
            case SQUID -> wardDrown(player, data, event, 1, 1, 1200, 300);
            case CREEPER -> absorbLightning(player, data, event);
            default -> { }
        }
    }

    private static void wardFire(ServerPlayer player, WitcheryPlayerData data, LivingIncomingDamageEvent event,
                                 int duration, boolean skipIfActive) {
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }
        if (skipIfActive && player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return;
        }
        if (spendEnergy(player, data, 3)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, true, false));
            ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.6F, 1.0F);
            event.setCanceled(true);
        }
    }

    private static void wardDrown(ServerPlayer player, WitcheryPlayerData data, LivingIncomingDamageEvent event,
                                  int cost, int amplifier, int duration, int airSupply) {
        if (event.getSource().is(DamageTypes.DROWN) && spendEnergy(player, data, cost)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, amplifier, true, false));
            player.setAirSupply(airSupply);
            ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.6F, 1.0F);
            event.setCanceled(true);
        }
    }

    private static void absorbLightning(ServerPlayer player, WitcheryPlayerData data, LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
            return;
        }
        data.addCreaturePowerCharges(25);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY(1.0), player.getZ(), 24, 0.4, 0.6, 0.4, 0.1);
        event.setCanceled(true);
    }

    private static boolean isOverworldFallGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SNOW) || state.is(Blocks.DIRT);
    }

    private static boolean spendEnergy(ServerPlayer player, WitcheryPlayerData data, int amount) {
        if (player.isCreative()) {
            return true;
        }
        int current = data.getInfusionEnergy();
        if (current < amount) {
            return false;
        }
        data.setInfusionEnergy(current - amount, data.getInfusionMaxEnergy());
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        return true;
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
