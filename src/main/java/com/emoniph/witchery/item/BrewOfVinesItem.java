package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Grows vines up and down vertical surfaces hit by the brew. */
public class BrewOfVinesItem extends BrewItem {
    public BrewOfVinesItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        // Find solid blocks within 6 blocks and grow vines on their exposed faces
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                for (int dy = -1; dy <= 8; dy++) {
                    BlockPos bp = center.offset(dx, dy, dz);
                    if (!level.getBlockState(bp).isSolid()) continue;
                    // Grow vines downward on south face
                    for (int drop = 1; drop <= 8; drop++) {
                        BlockPos vine = bp.below(drop);
                        if (!level.getBlockState(vine).isAir()) break;
                        var vineState = Blocks.VINE.defaultBlockState()
                                .setValue(VineBlock.SOUTH, true);
                        level.setBlockAndUpdate(vine, vineState);
                        level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                                vine.getX() + 0.5, vine.getY() + 0.5, vine.getZ() + 0.5,
                                0, 0.05, 0);
                    }
                    break;
                }
            }
        }
    }
}
