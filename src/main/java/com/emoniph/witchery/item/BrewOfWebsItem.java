package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Explodes into cobwebs — creates a 4-block-radius sphere of webs at the impact point. */
public class BrewOfWebsItem extends BrewItem {
    public BrewOfWebsItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx*dx + dy*dy + dz*dz > radius*radius) continue;
                    BlockPos bp = center.offset(dx, dy, dz);
                    if (level.getBlockState(bp).isAir()) {
                        if (level.getRandom().nextFloat() < 0.4f) {
                            level.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                            if (level.getRandom().nextFloat() < 0.3f)
                                level.addParticle(ParticleTypes.CLOUD,
                                        bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5,
                                        0, 0.04, 0);
                        }
                    }
                }
            }
        }
        level.playSound(null, center, SoundEvents.SPIDER_AMBIENT,
                SoundSource.PLAYERS, 0.8f, 0.6f);
    }
}
