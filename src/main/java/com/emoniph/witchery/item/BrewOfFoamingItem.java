package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brew of Foaming — covers the ground around the drinker with cobwebs (foam),
 * trapping mobs and slowing movement in a 4-block radius.
 */
public class BrewOfFoamingItem extends BrewItem {

    public BrewOfFoamingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 20) continue; // rough circle
                BlockPos floor = center.offset(dx, 0, dz);
                BlockState above = level.getBlockState(floor);
                // Only place on air; don't overwrite solid blocks
                if (above.isAir()) {
                    level.setBlockAndUpdate(floor, Blocks.COBWEB.defaultBlockState());
                }
            }
        }
    }
}
