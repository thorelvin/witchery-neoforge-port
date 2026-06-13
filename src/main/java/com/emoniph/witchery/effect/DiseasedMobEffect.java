package com.emoniph.witchery.effect;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

/**
 * Source 1.7.10 Diseased: harmful, incurable, weakens attack damage, and
 * periodically leaves disease patches below infected creatures.
 */
public class DiseasedMobEffect extends MobEffect {
    public static final int BREW_DURATION_TICKS = 3 * 60 * 20;
    public static final int NORMAL_CURE_STRENGTH = 2;
    public static final int BLOCK_CLEANUP_STRENGTH = 3;
    public static final int BLOCK_CLEANUP_RADIUS = 9;
    public static final int SOURCE_SPREAD_TICK_INTERVAL = 40;
    public static final int SOURCE_SPREAD_TICK_PHASE = 4;
    public static final int SOURCE_SPREAD_CHANCE = 3;
    public static final int SOURCE_CONTACT_MIN_DURATION_TICKS = 60 * 20;
    public static final int SOURCE_CONTACT_RANDOM_EXTRA_MINUTES = 4;
    private static final Identifier ATTACK_DAMAGE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "effect.diseased_attack_damage");

    public DiseasedMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return shouldSourceSpreadOnTick(duration);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        syncSourceAttackDamageModifier(entity, amplifier);
        tryPlaceDiseaseBlock(level, entity);
        return true;
    }

    public static void syncSourceAttackDamageModifier(LivingEntity entity, int amplifier) {
        AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.addOrUpdateTransientModifier(new AttributeModifier(ATTACK_DAMAGE_MODIFIER_ID,
                    sourceAttackDamagePenalty(amplifier), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void removeSourceAttackDamageModifier(LivingEntity entity) {
        AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);
        }
    }

    public static double sourceAttackDamagePenalty(int amplifier) {
        return -0.5D * (double) (Math.min(Math.max(0, amplifier), 1) + 1);
    }

    public static boolean shouldSourceSpreadOnTick(int remainingDuration) {
        return remainingDuration % SOURCE_SPREAD_TICK_INTERVAL == SOURCE_SPREAD_TICK_PHASE;
    }

    public static boolean shouldSourceAttemptSpread(RandomSource random) {
        return random.nextInt(SOURCE_SPREAD_CHANCE) == 0;
    }

    public static int sourceContactDurationTicks(RandomSource random) {
        return sourceContactDurationTicks(random.nextInt(SOURCE_CONTACT_RANDOM_EXTRA_MINUTES));
    }

    public static int sourceContactDurationTicks(int randomExtraMinutes) {
        return SOURCE_CONTACT_MIN_DURATION_TICKS
                + Math.max(0, randomExtraMinutes) * 60 * 20;
    }

    public static boolean canPlaceDiseaseAtFeet(ServerLevel level, LivingEntity entity, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        return !(entity instanceof Player player) || level.mayInteract(player, pos);
    }

    public static boolean tryPlaceDiseaseBlock(ServerLevel level, LivingEntity entity) {
        if (!shouldSourceAttemptSpread(level.random)) {
            return false;
        }
        BlockPos pos = entity.blockPosition();
        if (!canPlaceDiseaseAtFeet(level, entity, pos)) {
            return false;
        }
        level.setBlock(pos, WitcheryBlocks.DISEASE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.scheduleTick(pos, WitcheryBlocks.DISEASE.get(), com.emoniph.witchery.blocks.DiseaseBlock.SOURCE_TICK_RATE);
        return true;
    }
}
