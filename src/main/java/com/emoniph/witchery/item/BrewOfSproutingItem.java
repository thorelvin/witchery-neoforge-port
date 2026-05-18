package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brew of Sprouting — grows a branch of oak logs extending from the nearest solid surface.
 * Great for instant bridges or emergency elevation.
 */
public class BrewOfSproutingItem extends BrewItem {
    public BrewOfSproutingItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        Direction facing = player.getDirection();

        // Find the surface the player is looking toward
        BlockPos start = center;
        for (int i = 0; i < 3; i++) {
            if (level.getBlockState(start).isSolid()) break;
            start = start.relative(facing.getOpposite());
        }

        // Grow 8 oak logs extending in the facing direction
        BlockState log = Blocks.OAK_LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, facing.getAxis());
        for (int i = 0; i < 8; i++) {
            BlockPos bp = start.relative(facing, i);
            if (level.getBlockState(bp).isAir() || level.getBlockState(bp).liquid()) {
                level.setBlockAndUpdate(bp, log);
            } else {
                break;
            }
        }
    }
}
