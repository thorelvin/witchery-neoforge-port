package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Internal `brew_of_ice` item, displayed as Brew of Frost like original Witchery. */
public class BrewOfIceItem extends BrewItem {
    public BrewOfIceItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        freezeNearbyWater(level, center, 3);
        createIceShell(level, center, 3);
    }

    private static void freezeNearbyWater(Level level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos bp = center.offset(dx, dy, dz);
                    if (level.getBlockState(bp).is(Blocks.WATER)) {
                        level.setBlockAndUpdate(bp, Blocks.ICE.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void createIceShell(Level level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > radius * radius || distSq < (radius - 1) * (radius - 1)) {
                        continue;
                    }
                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && dy >= 0 && dy <= 1) {
                        continue;
                    }
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
