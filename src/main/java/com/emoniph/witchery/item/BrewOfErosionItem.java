package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Brew of Erosion — degrades stone/cobblestone to gravel and dirt to coarse dirt in a 3-block radius. */
public class BrewOfErosionItem extends BrewItem {

    public BrewOfErosionItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        BlockPos center = player.blockPosition();
        boolean brewMastery = FamiliarHelper.hasActiveToadFamiliar(player);
        float creatureDamage = brewMastery ? 6.0F : 4.0F;

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

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(3.0))) {
            if (target == player) {
                continue;
            }
            target.hurt(player.damageSources().magic(), creatureDamage);
        }
    }
}
