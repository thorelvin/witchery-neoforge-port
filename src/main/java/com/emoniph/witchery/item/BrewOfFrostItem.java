package com.emoniph.witchery.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Brew of Frost — freezes nearby water, slows enemies. */
public class BrewOfFrostItem extends BrewItem {

    public BrewOfFrostItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        // Freeze the player briefly and apply slowness
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,      100, 1));
        // Freeze nearby water blocks in a 3-block radius
        net.minecraft.core.BlockPos center = player.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    net.minecraft.core.BlockPos bp = center.offset(dx, dy, dz);
                    net.minecraft.world.level.block.state.BlockState st = level.getBlockState(bp);
                    if (st.is(net.minecraft.world.level.block.Blocks.WATER)) {
                        level.setBlockAndUpdate(bp, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState());
                    }
                }
            }
        }
    }
}
