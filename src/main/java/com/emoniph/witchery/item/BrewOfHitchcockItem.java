package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.RavenEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Brew of Hitchcock — a reference to "The Birds". Releases a panicked flock of birds
 * that scatter around the area, causing general mayhem.
 */
public class BrewOfHitchcockItem extends BrewItem {
    public static final int DEFAULT_RAVEN_COUNT = 12;
    public static final double DEFAULT_RAVEN_SPREAD = 8.0;

    public BrewOfHitchcockItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        spawnRavenFlock(level, player.getX(), player.getY() + 1.0, player.getZ(), DEFAULT_RAVEN_COUNT, DEFAULT_RAVEN_SPREAD);
    }

    public static int spawnRavenFlock(Level level, double centerX, double centerY, double centerZ, int count, double spread) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            RavenEntity bird = WitcheryEntities.RAVEN.get().create(level, EntitySpawnReason.TRIGGERED);
            if (bird == null) {
                continue;
            }
            double ox = (level.getRandom().nextDouble() - 0.5) * spread;
            double oz = (level.getRandom().nextDouble() - 0.5) * spread;
            bird.setPos(centerX + ox, centerY, centerZ + oz);
            bird.setYRot(level.getRandom().nextFloat() * 360.0F);
            // Fling birds in random directions.
            bird.setDeltaMovement(
                (level.getRandom().nextDouble() - 0.5) * 0.8,
                level.getRandom().nextDouble() * 0.6,
                (level.getRandom().nextDouble() - 0.5) * 0.8
            );
            level.addFreshEntity(bird);
            level.addParticle(ParticleTypes.CLOUD,
                    centerX + ox, centerY, centerZ + oz,
                    0, 0.05, 0);
            spawned++;
        }
        return spawned;
    }
}
