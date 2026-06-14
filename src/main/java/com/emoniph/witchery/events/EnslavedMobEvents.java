package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.util.Enslaved;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Drives the AI side of {@link Enslaved} thralls, replacing source {@code PotionEnslaved}'s event hook
 * ({@code onLivingSetAttackTarget}) and the {@code EntityAIEnslaverHurtByTarget} goal:
 * <ul>
 *   <li>an enslaved mob never targets its own enslaver;</li>
 *   <li>when the enslaver is hurt, nearby thralls round on the attacker (the "revenge" behaviour).</li>
 * </ul>
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class EnslavedMobEvents {

    /** Radius around a hurt enslaver within which thralls answer the call. Matches the command radius. */
    private static final double REVENGE_RADIUS = 50.0D;

    private EnslavedMobEvents() {}

    /** Source {@code PotionEnslaved.onLivingSetAttackTarget}: a thrall will not attack its enslaver. */
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !Enslaved.isEnslaved(mob)) {
            return;
        }
        if (event.getNewAboutToBeSetTarget() instanceof Player player && Enslaved.isEnslavedBy(mob, player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Source {@code EntityAIEnslaverHurtByTarget}: when the enslaver is struck, thralls retaliate. */
    @SubscribeEvent
    public static void onEnslaverHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player enslaver)
                || !(enslaver.level() instanceof ServerLevel level)) {
            return;
        }
        Entity raw = event.getSource().getEntity();
        if (!(raw instanceof LivingEntity attacker) || attacker == enslaver) {
            return;
        }

        AABB area = enslaver.getBoundingBox().inflate(REVENGE_RADIUS, 15.0D, REVENGE_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, m -> Enslaved.isEnslavedBy(m, enslaver))) {
            if (mob != attacker && Enslaved.canBeEnslaved(mob)) {
                mob.setTarget(attacker);
            }
        }
    }
}
