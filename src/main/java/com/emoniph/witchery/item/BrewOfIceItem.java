package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Brew of Ice — creates a protective dome of ice around the drinker. */
public class BrewOfIceItem extends BrewItem {
    public BrewOfIceItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        int radius = 3;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Shell only — skip interior
                    int distSq = dx*dx + dy*dy + dz*dz;
                    if (distSq > radius*radius || distSq < (radius-1)*(radius-1)) continue;
                    // Don't place inside the player
                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && dy >= 0 && dy <= 1) continue;
                    BlockPos bp = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(bp);
                    if (st.isAir() || st.liquid()) {
                        level.setBlockAndUpdate(bp, Blocks.ICE.defaultBlockState());
                    }
                }
            }
        }
    }
}
