package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Brew of Substitution — swaps blocks in a spiral pattern around the player.
 * Solid blocks within the area are shuffled with nearby solid blocks.
 */
public class BrewOfSubstitutionItem extends BrewItem {
    public BrewOfSubstitutionItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        int radius = 4;

        // Collect all solid blocks in the radius
        List<BlockPos> solid = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos bp = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(bp);
                    if (st.isSolid() && !st.hasBlockEntity()) {
                        solid.add(bp);
                    }
                }
            }
        }

        if (solid.size() < 2) return;

        // Save states
        List<BlockState> states = new ArrayList<>();
        for (BlockPos bp : solid) states.add(level.getBlockState(bp));

        // Shuffle with a seeded random for consistency
        Random rng = new Random(center.asLong());
        for (int i = states.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            BlockState tmp = states.get(i);
            states.set(i, states.get(j));
            states.set(j, tmp);
        }

        for (int i = 0; i < solid.size(); i++) {
            level.setBlockAndUpdate(solid.get(i), states.get(i));
        }
    }
}
