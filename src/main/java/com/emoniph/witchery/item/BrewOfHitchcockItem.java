package com.emoniph.witchery.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Brew of Hitchcock — a reference to "The Birds". Releases a panicked flock of birds
 * that scatter around the area, causing general mayhem.
 */
public class BrewOfHitchcockItem extends BrewItem {
    public BrewOfHitchcockItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        for (int i = 0; i < 12; i++) {
            Parrot bird = EntityType.PARROT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (bird == null) continue;
            double ox = (level.getRandom().nextDouble() - 0.5) * 8;
            double oz = (level.getRandom().nextDouble() - 0.5) * 8;
            bird.setPos(player.getX() + ox, player.getY() + 1, player.getZ() + oz);
            // Fling birds in random directions
            bird.setDeltaMovement(
                (level.getRandom().nextDouble() - 0.5) * 0.8,
                level.getRandom().nextDouble() * 0.6,
                (level.getRandom().nextDouble() - 0.5) * 0.8
            );
            level.addFreshEntity(bird);
            level.addParticle(ParticleTypes.CLOUD,
                    player.getX() + ox, player.getY() + 1, player.getZ() + oz,
                    0, 0.05, 0);
        }
    }
}
