package com.emoniph.witchery.events;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.item.VampireClothingUtil;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles all vampire gameplay: blood drain, sunburn, passive effects, and biting.
 *
 * Blood thresholds (0–500):
 *   0–50   → frenzy: Weakness II + Hunger II
 *   51–150 → hungry: Weakness I
 *   151–349→ normal: Night Vision only
 *   350–500→ well-fed: Night Vision + Speed I + Strength I
 */
@EventBusSubscriber
public class VampireEvents {

    private static final int DRAIN_INTERVAL  = 100; // 1 blood lost per 5 seconds
    private static final int EFFECT_INTERVAL = 100;
    private static final int EFFECT_DURATION = 140; // slightly longer than interval
    private static final int SYNC_INTERVAL   = 200; // sync blood level every 10 seconds
    private static final int MESMERISE_BLOOD_COST = 50;
    private static final int MESMERISE_BASE_DURATION = 5 * 20;
    private static final int MESMERISE_BOOST_DURATION = 3 * 20;
    private static final int MESMERISE_SLOWNESS_AMPLIFIER = 4;

    // ── Tick ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) return;

        int tick = player.tickCount;
        if (tick % WitcheryConfig.VAMPIRE_BLOOD_DRAIN_INTERVAL.get() == 0) drainBlood(player, data);
        if (tick % WitcheryConfig.VAMPIRE_SUNBURN_CHECK_INTERVAL.get() == 0) checkSunburn(player);
        if (tick % EFFECT_INTERVAL == 0) applyBloodEffects(player, data);
        if (tick % SYNC_INTERVAL   == 0) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    // ── Bite ─────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer vampire)) return;
        if (!vampire.getMainHandItem().isEmpty()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        WitcheryPlayerData data = WitcheryAttachments.get(vampire);
        if (!data.isVampire()) return;

        if (vampire.isShiftKeyDown() && target instanceof Villager) {
            event.setCanceled(true);
            mesmerise(vampire, target, data);
            return;
        }
        if (target instanceof Player) return; // player biting deferred to M22+

        event.setCanceled(true);
        bite(vampire, target, data);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void drainBlood(ServerPlayer player, WitcheryPlayerData data) {
        data.addBlood(-1);
        if (data.getBloodLevel() == 50) {
            player.displayClientMessage(Component.translatable("witchery.vampire.frenzy"), false);
        }
    }

    private static void checkSunburn(ServerPlayer player) {
        if (player.level().getDayTime() % 24000 >= 12000) return;
        if (!player.level().canSeeSky(player.blockPosition().above())) return;
        if (player.isUnderWater() || player.isInWater()) return;
        if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return; // helmet blocks sunburn
        player.hurt(player.damageSources().inFire(), WitcheryConfig.VAMPIRE_SUNBURN_DAMAGE.get());
        if (player.tickCount % 100 == 0) {
            player.displayClientMessage(Component.translatable("witchery.vampire.sunburn"), false);
        }
    }

    private static void applyBloodEffects(ServerPlayer player, WitcheryPlayerData data) {
        int blood = data.getBloodLevel();
        if (blood <= 50) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 1, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER,   EFFECT_DURATION, 1, true, true));
        } else if (blood <= 150) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION, 0, true, false));
        } else if (blood >= 350) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,   EFFECT_DURATION, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,   EFFECT_DURATION, 0, true, false));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION, 0, true, false));
        }
    }

    private static void bite(ServerPlayer vampire, LivingEntity target, WitcheryPlayerData data) {
        int gained = bloodValue(vampire, target);
        target.hurt(vampire.damageSources().playerAttack(vampire), 2f);
        data.addBlood(gained);
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));

        vampire.level().playSound(null, vampire.blockPosition(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6f, 0.5f);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.bite.success",
                target.getName(), gained, data.getBloodLevel()), false);
    }

    private static void mesmerise(ServerPlayer vampire, LivingEntity target, WitcheryPlayerData data) {
        if (isMesmerised(target)) {
            playMesmeriseFailure(vampire);
            vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.already",
                    target.getName()), true);
            return;
        }
        if (data.getBloodLevel() < MESMERISE_BLOOD_COST) {
            playMesmeriseFailure(vampire);
            vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.not_enough_blood",
                    MESMERISE_BLOOD_COST), true);
            return;
        }

        int duration = MESMERISE_BASE_DURATION;
        if (VampireClothingUtil.isMesmeriseBoostActive(vampire)) {
            duration += MESMERISE_BOOST_DURATION;
        }

        data.addBlood(-MESMERISE_BLOOD_COST);
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, MESMERISE_SLOWNESS_AMPLIFIER, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, true));
        vampire.level().playSound(null, target.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.mesmerise.success",
                target.getName(), duration / 20), true);
    }

    private static boolean isMesmerised(LivingEntity target) {
        MobEffectInstance slowness = target.getEffect(MobEffects.SLOWNESS);
        return slowness != null
                && slowness.getAmplifier() >= MESMERISE_SLOWNESS_AMPLIFIER
                && slowness.getDuration() > 20;
    }

    private static void playMesmeriseFailure(ServerPlayer vampire) {
        vampire.level().playSound(null, vampire.blockPosition(),
                SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5F, 0.5F);
    }

    /** Blood gained from biting different entity types. */
    private static int bloodValue(ServerPlayer vampire, LivingEntity entity) {
        int base;
        if (entity instanceof Monster) {
            base = 5;   // undead/hostile have thin blood
        } else if (entity instanceof Animal) {
            base = 25;  // animals are a good feed
        } else {
            base = 15;
        }
        if (!VampireClothingUtil.isDrinkBoostActive(vampire)) {
            return base;
        }
        return (base * 3 + 1) / 2;
    }
}
