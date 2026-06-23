package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BrewFluidBlockEntity;
import com.emoniph.witchery.init.WitcheryFluids;
import com.emoniph.witchery.item.BrewContainers;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BrewLiquidBlock extends LiquidBlock implements EntityBlock {
    public BrewLiquidBlock(BlockBehaviour.Properties properties) {
        super(WitcheryFluids.BREW_LIQUID_SOURCE.get(), properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewFluidBlockEntity(pos, state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (level.isClientSide()
                || !(entity instanceof LivingEntity living)
                || !(level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew)
                || level.random.nextInt(10) != 4) {
            return;
        }
        brew.applyContactEffect(living);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity entity, LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(LEVEL) != 0) {
            return ItemStack.EMPTY;
        }
        if (!(level.getBlockEntity(pos) instanceof BrewFluidBlockEntity brew) || brew.getContents().isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = BrewContainers.bucket(brew.getContents());
        level.removeBlock(pos, false);
        return stack;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return WitcheryFluids.BREW_LIQUID_SOURCE.get().getPickupSound();
    }
}
