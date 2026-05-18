package com.emoniph.witchery.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Brew of Frog's Tongue — drags all nearby players and entities toward the thrower. */
public class BrewOfFrogsTongueItem extends BrewItem {
    public BrewOfFrogsTongueItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(12);
        Vec3 origin = player.position().add(0, 1, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e == player) continue;
            Vec3 pull = origin.subtract(e.position()).normalize().scale(1.8);
            e.setDeltaMovement(pull.x, Math.max(pull.y, 0.4), pull.z);
            e.hurtMarked = true;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FROG_TONGUE,
                SoundSource.PLAYERS, 1f, 0.7f);
    }
}
