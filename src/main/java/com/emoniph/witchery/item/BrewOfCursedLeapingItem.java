package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Brew of Cursed Leaping — applies Jump Boost III and flings nearby entities skyward.
 * The leap is too strong to be safe; fall damage follows.
 */
public class BrewOfCursedLeapingItem extends BrewItem {
    public BrewOfCursedLeapingItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(8);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            e.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 100, 2));
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
            Vec3 vel = e.getDeltaMovement();
            e.setDeltaMovement(vel.x, 1.8, vel.z);
            e.hurtMarked = true;
        }
    }
}
