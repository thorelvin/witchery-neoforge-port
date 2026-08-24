package com.emoniph.witchery.util;

import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.network.SyncVampireTargetBloodPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared source-style blood pool for ordinary villagers. */
public final class VillagerBlood {
    public static final int MAX = 500;
    public static final int LOW = 250;
    public static final String TAG = "witchery_villager_blood";

    private static final int TRANSFIXED_AMPLIFIER = 4;

    private VillagerBlood() {
    }

    public static int get(Villager villager) {
        return Math.clamp(villager.getPersistentData().getIntOr(TAG, MAX), 0, MAX);
    }

    public static void set(Villager villager, int blood) {
        int clamped = Math.clamp(blood, 0, MAX);
        villager.getPersistentData().putInt(TAG, clamped);
        if (!villager.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntity(villager,
                    new SyncVampireTargetBloodPayload(villager.getId(), clamped));
        }
    }

    public static void give(Villager villager, int quantity) {
        if (quantity > 0) {
            set(villager, get(villager) + quantity);
        }
    }

    public static int take(Villager villager, int quantity) {
        int stored = get(villager);
        int desired = sourceAdjustedDrain(quantity, isKnockedOut(villager));
        int taken = Math.min(stored, desired);
        set(villager, stored - taken);
        return taken;
    }

    public static boolean isKnockedOut(Villager villager) {
        MobEffectInstance paralysis = villager.getEffect(WitcheryMobEffects.PARALYSED);
        return villager.isSleeping()
                || paralysis != null && paralysis.getAmplifier() >= TRANSFIXED_AMPLIFIER;
    }

    public static void recoverAfterSleep(Villager villager, int sleepingTicks) {
        give(villager, sourceSleepRecovery(sleepingTicks));
        if (sourceSleepFullyHeals(sleepingTicks)) {
            villager.setHealth(villager.getMaxHealth());
        }
    }

    static int sourceAdjustedDrain(int quantity, boolean knockedOut) {
        int positive = Math.max(0, quantity);
        return knockedOut ? positive : (int) Math.ceil(positive * 0.66F);
    }

    static int sourceSleepRecovery(int sleepingTicks) {
        return sleepingTicks > 20 * 60 ? 50 * (sleepingTicks / (20 * 60)) : 0;
    }

    static boolean sourceSleepFullyHeals(int sleepingTicks) {
        return sleepingTicks >= 20 * 60 * 2;
    }
}
