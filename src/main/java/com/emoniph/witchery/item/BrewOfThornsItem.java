package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Instantly grows cacti at four blocks high wherever the brew lands. */
public class BrewOfThornsItem extends BrewItem {
    public BrewOfThornsItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        int height = FamiliarHelper.hasActiveToadFamiliar(player) ? 4 : 3;

        // Grow a ring of cacti around the player.
        int[][] ring = {{3,0},{-3,0},{0,3},{0,-3},{2,2},{-2,2},{2,-2},{-2,-2}};
        for (int[] off : ring) {
            BlockPos base = center.offset(off[0], 0, off[1]);
            // Find ground
            BlockPos ground = base;
            for (int tries = 0; tries < 4; tries++) {
                if (level.getBlockState(ground.below()).isSolid()) break;
                ground = ground.below();
            }
            if (!level.getBlockState(ground.below()).isSolid()) continue;
            if (!level.getBlockState(ground.below()).is(Blocks.SAND)
                    && !level.getBlockState(ground.below()).is(Blocks.RED_SAND)) {
                level.setBlockAndUpdate(ground.below(), Blocks.SAND.defaultBlockState());
            }
            for (int h = 0; h < height; h++) {
                BlockPos bp = ground.above(h);
                if (level.getBlockState(bp).isAir()) {
                    level.setBlockAndUpdate(bp, Blocks.CACTUS.defaultBlockState());
                }
            }
        }
    }
}
