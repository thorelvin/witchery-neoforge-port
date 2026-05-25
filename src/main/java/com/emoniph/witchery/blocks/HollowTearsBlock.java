package com.emoniph.witchery.blocks;

import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.NightmareEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Source-style Hollow Tears stand-in until the custom fluid system is rebuilt. */
public class HollowTearsBlock extends Block {
    public HollowTearsBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (level.isClientSide() || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        MobEffectInstance effect = isSpiritTarget(livingEntity)
                ? new MobEffectInstance(MobEffects.REGENERATION, 100, 1)
                : new MobEffectInstance(MobEffects.WEAKNESS, 100, 1);

        if (!livingEntity.hasEffect(effect.getEffect())) {
            livingEntity.addEffect(effect);
        }
    }

    private static boolean isSpiritTarget(LivingEntity livingEntity) {
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
