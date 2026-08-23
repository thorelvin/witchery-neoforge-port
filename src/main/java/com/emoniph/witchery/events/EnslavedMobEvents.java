package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.util.Enslaved;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Drives the AI side of {@link Enslaved} thralls, replacing source {@code PotionEnslaved}'s event hook
 * ({@code onLivingSetAttackTarget}) and the {@code EntityAIEnslaverHurtByTarget} goal:
 * <ul>
 *   <li>an enslaved mob never targets its own enslaver;</li>
 *   <li>when the enslaver is hurt, loaded pathfinding thralls in that dimension round on the attacker.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class EnslavedMobEvents {

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
    public static void onEnslaverHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player enslaver)
                || !(enslaver.level() instanceof ServerLevel level)
                || event.getNewDamage() <= 0.0F) {
            return;
        }
        Entity raw = event.getSource().getEntity();
        if (!(raw instanceof LivingEntity attacker) || attacker == enslaver) {
            return;
        }

        alertThralls(level, enslaver, attacker);
    }

    /** Event-driven equivalent of each source creature polling its enslaver's revenge timer. */
    public static int alertThralls(ServerLevel level, Player enslaver, LivingEntity attacker) {
        int alerted = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof PathfinderMob mob
                    && mob != attacker
                    && Enslaved.hasActiveBond(mob)
                    && Enslaved.isEnslavedBy(mob, enslaver)
                    && mob.canAttack(attacker)) {
                mob.setTarget(attacker);
                alerted++;
            }
        }
        return alerted;
    }
}
