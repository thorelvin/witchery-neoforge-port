package com.emoniph.witchery.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Witchery's leaping lily is a permissive lily pad that briefly boosts speed
 * and jumping when walked over.
 */
public class LeapingLilyBlock extends WaterlilyBlock {

    public LeapingLilyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        FluidState fluid = state.getFluidState();
        return fluid.is(Fluids.WATER) || state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, 0, true, false));
            living.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 10, 4, true, false));
        }
    }
}
