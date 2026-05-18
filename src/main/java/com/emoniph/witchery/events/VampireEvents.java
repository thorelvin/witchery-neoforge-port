package com.emoniph.witchery.events;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
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
        if (target instanceof Player) return; // player biting deferred to M22+

        WitcheryPlayerData data = WitcheryAttachments.get(vampire);
        if (!data.isVampire()) return;

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
        int gained = bloodValue(target);
        target.hurt(vampire.damageSources().playerAttack(vampire), 2f);
        data.addBlood(gained);
        PacketDistributor.sendToPlayer(vampire, SyncPlayerDataPayload.from(vampire, data));

        vampire.level().playSound(null, vampire.blockPosition(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6f, 0.5f);
        vampire.displayClientMessage(Component.translatable("witchery.vampire.bite.success",
                target.getName(), gained, data.getBloodLevel()), false);
    }

    /** Blood gained from biting different entity types. */
    private static int bloodValue(LivingEntity entity) {
        if (entity instanceof Monster) return 5;   // undead/hostile have thin blood
        if (entity instanceof Animal)  return 25;  // animals are a good feed
        return 15;
    }
}
