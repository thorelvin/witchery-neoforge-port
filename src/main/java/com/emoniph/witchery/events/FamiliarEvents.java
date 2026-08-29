package com.emoniph.witchery.events;

import com.emoniph.witchery.entity.familiar.CreakingFamiliarHelper;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public final class FamiliarEvents {

    private static final double NEAR_FAMILIAR_DISTANCE_SQ = 576.0;
    private static final float REDIRECTED_DAMAGE_PCT_FAR = 0.01F;
    private static final float REDIRECTED_DAMAGE_PCT_NEAR = 0.10F;

    private FamiliarEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getNewDamage() <= 0.0F) {
            return;
        }

        LivingEntity familiar = FamiliarHelper.findBoundFamiliarEntity(player);
        if (familiar == null || !familiar.isAlive()) {
            return;
        }
        if (familiar instanceof Creaking creaking && CreakingFamiliarHelper.isRootedGuard(creaking)) {
            return;
        }

        float redirectedDamage = event.getNewDamage() * REDIRECTED_DAMAGE_PCT_FAR;
        if (familiar.distanceToSqr(player) <= NEAR_FAMILIAR_DISTANCE_SQ) {
            redirectedDamage = event.getNewDamage() * REDIRECTED_DAMAGE_PCT_NEAR;
        }

        if (redirectedDamage >= 1.0F) {
            familiar.hurt(event.getSource(), redirectedDamage);
            event.setNewDamage(Math.max(0.0F, event.getNewDamage() - redirectedDamage));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LivingEntity familiar = FamiliarHelper.findBoundFamiliarEntity(player);
            if (familiar instanceof TamableAnimal tameable) {
                FamiliarHelper.markFamiliarDismissed(player, tameable, true);
            } else if (familiar instanceof Creaking creaking) {
                CreakingFamiliarHelper.markDismissed(player, creaking, true);
            }
            return;
        }

        if (!(event.getEntity() instanceof FamiliarEntity familiar)) {
            return;
        }

        ServerPlayer owner = FamiliarHelper.findBoundOwner(familiar);
        if (owner == null) {
            return;
        }

        FamiliarHelper.markFamiliarDismissed(owner, familiar, false);
        owner.hurt(owner.damageSources().magic(), owner.getMaxHealth() * 2.0F);
    }
}
