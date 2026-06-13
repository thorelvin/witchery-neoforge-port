package com.emoniph.witchery.blocks;

import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.effect.DiseasedMobEffect;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Source {@code BlockDisease}: a one-quanta disease patch that decays at random
 * and infects vulnerable living entities that touch it.
 */
public class DiseaseBlock extends Block {
    public static final MapCodec<DiseaseBlock> CODEC = simpleCodec(DiseaseBlock::new);
    public static final int SOURCE_TICK_RATE = 10;
    public static final int SOURCE_REMOVAL_CHANCE = 10;
    public static final int SOURCE_CONTACT_CHANCE = 3;

    public DiseaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, SOURCE_TICK_RATE);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (shouldSourceRemove(random)) {
            level.removeBlock(pos, false);
            return;
        }
        level.scheduleTick(pos, this, SOURCE_TICK_RATE);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier effectApplier, boolean isMoved) {
        if (level.isClientSide()
                || !(entity instanceof LivingEntity living)
                || Brews.isSourceDiseaseImmune(living)
                || living.hasEffect(WitcheryMobEffects.DISEASED)
                || !shouldSourceInfect(level.random)) {
            return;
        }
        living.addEffect(new MobEffectInstance(WitcheryMobEffects.DISEASED,
                DiseasedMobEffect.sourceContactDurationTicks(level.random), 0, true, true));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return true;
    }

    public static boolean shouldSourceRemove(RandomSource random) {
        return shouldSourceRemove(random.nextInt(SOURCE_REMOVAL_CHANCE));
    }

    public static boolean shouldSourceRemove(int roll) {
        return roll == 0;
    }

    public static boolean shouldSourceInfect(RandomSource random) {
        return shouldSourceInfect(random.nextInt(SOURCE_CONTACT_CHANCE));
    }

    public static boolean shouldSourceInfect(int roll) {
        return roll == 0;
    }
}
