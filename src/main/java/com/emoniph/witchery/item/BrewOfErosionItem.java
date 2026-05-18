package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Brew of Erosion — degrades stone/cobblestone to gravel and dirt to coarse dirt in a 3-block radius. */
public class BrewOfErosionItem extends BrewItem {

    public BrewOfErosionItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                            || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.DEEPSLATE)) {
                        level.setBlockAndUpdate(pos, Blocks.GRAVEL.defaultBlockState());
                    } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                        level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
                    } else if (state.is(Blocks.SAND)) {
                        level.setBlockAndUpdate(pos, Blocks.GRAVEL.defaultBlockState());
                    }
                }
            }
        }
    }
}
