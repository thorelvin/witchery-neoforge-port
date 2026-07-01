package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.ChalkCircleBlock;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Brew of Wasting - hunger/weakness/wither burst plus fragile-plant decay. */
public class BrewOfWastingItem extends BrewItem {
    private static final double RADIUS = 4.0D;
    private static final int BLOCK_RADIUS = 3;

    public BrewOfWastingItem(Properties properties) { super(properties); }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB area = new AABB(player.blockPosition()).inflate(RADIUS, 2.0D, RADIUS);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.distanceToSqr(player) < RADIUS * RADIUS);
        for (LivingEntity target : targets) {
            applyWasting(serverLevel, player, target);
        }
        witherFragileBlocks(serverLevel, player, player.blockPosition());
    }

    private static void applyWasting(ServerLevel level, Player center, LivingEntity target) {
        double distance = Math.sqrt(target.distanceToSqr(center));
        double strength = 1.0D - Math.min(distance, RADIUS) / RADIUS;
        if (target == center) {
            strength = 1.0D;
        }

        int scaled = (int) (strength * 400.0D + 0.5D);
        int weaknessTicks = Math.max(scaled / 3, 40);
        if (target instanceof ServerPlayer victim) {
            if (victim.getFoodData().getFoodLevel() > 10) {
                victim.getFoodData().setFoodLevel(Math.max(0, victim.getFoodData().getFoodLevel() - 10));
            }
            victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, scaled * 2, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessTicks, 0));
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, scaled * 2, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessTicks, 0));
        }

        level.sendParticles(ParticleTypes.SMOKE,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                6, target.getBbWidth() * 0.35D, target.getBbHeight() * 0.25D,
                target.getBbWidth() * 0.35D, 0.02D);
    }

    private static void witherFragileBlocks(ServerLevel level, Player player, BlockPos center) {
        int radiusSq = BLOCK_RADIUS * BLOCK_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-BLOCK_RADIUS, -BLOCK_RADIUS, -BLOCK_RADIUS),
                center.offset(BLOCK_RADIUS, BLOCK_RADIUS, BLOCK_RADIUS))) {
            if (pos.distSqr(center) > radiusSq) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (isFragilePlantMaterial(state)
                    && state.getDestroySpeed(level, pos) >= 0.0F
                    && WitcheryTerrainProtection.canBreak(player, level, pos)) {
                level.destroyBlock(pos, false);
            }
        }
    }

    private static boolean isFragilePlantMaterial(BlockState state) {
        if (state.isAir() || state.liquid() || state.hasBlockEntity() || state.getBlock() instanceof ChalkCircleBlock) {
            return false;
        }

        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.CAVE_VINES)
                || state.is(Blocks.VINE)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS);
    }
}
