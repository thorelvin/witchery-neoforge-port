package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;

/** Grows vines up and down vertical surfaces hit by the brew. */
public class BrewOfVinesItem extends BrewItem {
    public BrewOfVinesItem(Properties p) { super(p); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        boolean brewMastery = FamiliarHelper.hasActiveToadFamiliar(player);
        int horizontalRange = brewMastery ? 8 : 6;
        int dropLength = brewMastery ? 10 : 8;

        // Find solid blocks nearby and grow vines hanging from them.
        for (int dx = -horizontalRange; dx <= horizontalRange; dx++) {
            for (int dz = -horizontalRange; dz <= horizontalRange; dz++) {
                for (int dy = -1; dy <= 8; dy++) {
                    BlockPos bp = center.offset(dx, dy, dz);
                    if (!level.getBlockState(bp).isSolid()) continue;
                    for (int drop = 1; drop <= dropLength; drop++) {
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
