package com.emoniph.witchery.blocks;

import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryFluids;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

/**
 * Old Witchery let nightmare-bane flowing spirit cleanse disturbed cotton back
 * into wispy cotton and applied regeneration/weakness depending on the creature
 * standing in it.
 */
public class FlowingSpiritBlock extends LiquidBlock {

    public FlowingSpiritBlock(Properties properties) {
        super(WitcheryFluids.FLOWING_SPIRIT_SOURCE.get(), properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide() || oldState.is(state.getBlock()) || !level.dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
            return;
        }

        if (!level.getBlockState(pos.below()).is(Blocks.SNOW_BLOCK)) {
            return;
        }

        WitcheryBlocks.SPIRIT_PORTAL.get().tryToCreatePortal(level, pos);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide()) {
            return;
        }

        if (entity instanceof LivingEntity livingEntity) {
            applyLivingEffect(livingEntity);
            return;
        }

        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.is(WitcheryItems.DISTURBED_COTTON.get())) {
            itemEntity.setItem(new ItemStack(WitcheryItems.WISPY_COTTON.get(), stack.getCount()));
        }
    }

    private static void applyLivingEffect(LivingEntity livingEntity) {
        MobEffectInstance effect = isHarmTarget(livingEntity)
                ? new MobEffectInstance(MobEffects.WEAKNESS, 300, 1)
                : new MobEffectInstance(MobEffects.REGENERATION, 100, 1);

        if (!livingEntity.hasEffect(effect.getEffect())) {
            livingEntity.addEffect(effect);
        }
    }

    private static boolean isHarmTarget(LivingEntity livingEntity) {
        return livingEntity.getType().is(EntityTypeTags.UNDEAD)
                || livingEntity instanceof DemonEntity
                || livingEntity instanceof FlameImpEntity
                || livingEntity instanceof LordOfTormentEntity
                || livingEntity instanceof NightmareEntity
                || livingEntity.getType() == EntityType.BLAZE
                || livingEntity.getType() == EntityType.GHAST
                || livingEntity.getType() == EntityType.MAGMA_CUBE
                || livingEntity.getType() == EntityType.WITHER;
    }
}
