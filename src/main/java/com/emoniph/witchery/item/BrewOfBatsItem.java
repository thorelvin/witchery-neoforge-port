package com.emoniph.witchery.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Summons a cloud of bats that swarm nearby entities and apply Slowness. */
public class BrewOfBatsItem extends BrewItem {
    public BrewOfBatsItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        // Slowness on all nearby entities
        AABB area = new AABB(player.blockPosition()).inflate(8);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 1));
        }
        // Spawn 8 bats
        for (int i = 0; i < 8; i++) {
            Bat bat = EntityType.BAT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (bat == null) continue;
            double ox = (level.getRandom().nextDouble() - 0.5) * 6;
            double oz = (level.getRandom().nextDouble() - 0.5) * 6;
            bat.setPos(player.getX() + ox, player.getY() + 1, player.getZ() + oz);
            level.addFreshEntity(bat);
            level.addParticle(ParticleTypes.POOF,
                    player.getX() + ox, player.getY() + 1, player.getZ() + oz,
                    0, 0.1, 0);
        }
    }
}
