package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Brew of Ink - source-style compact blindness burst in practical item form. */
public class BrewOfInkItem extends BrewItem {
    private static final double NORMAL_RADIUS = 4.0D;
    private static final double ENHANCED_RADIUS = 5.0D;

    public BrewOfInkItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        BlockPos center = blockImpactPosition(serverLevel, context.getClickedPos(), context.getClickedFace());
        explodeInk(serverLevel, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                null, hasBrewMastery(player));
        consumeBrew(player, context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        explodeInk(serverLevel, target.getX(), target.getY(), target.getZ(), target, hasBrewMastery(player));
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            explodeInk(serverLevel, player.getX(), player.getY(), player.getZ(), player, hasBrewMastery(player));
        }
    }

    private static boolean hasBrewMastery(Player player) {
        return player != null && FamiliarHelper.hasActiveToadFamiliar(player);
    }

    private static BlockPos blockImpactPosition(ServerLevel level, BlockPos clickedPos, Direction face) {
        BlockPos pos = clickedPos.relative(face);
        if (face == Direction.UP && !level.getBlockState(clickedPos).isSolid()) {
            return pos.below();
        }
        return pos;
    }

    private static void explodeInk(ServerLevel level, double x, double y, double z,
                                   LivingEntity directTarget, boolean enhanced) {
        double radius = enhanced ? ENHANCED_RADIUS : NORMAL_RADIUS;
        double radiusSq = radius * radius;
        AABB area = new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D)
                .inflate(radius, 2.0D, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double distanceSq = target.distanceToSqr(x, y, z);
            if (distanceSq >= radiusSq) {
                continue;
            }
            double strength = 1.0D - Math.sqrt(distanceSq) / radius;
            if (target == directTarget) {
                strength = 1.0D;
            }

            int duration = Math.max(1, (int)(strength * 400.0D + 0.5D));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }

            level.sendParticles(ParticleTypes.SQUID_INK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    8, target.getBbWidth() * 0.35D, target.getBbHeight() * 0.25D,
                    target.getBbWidth() * 0.35D, 0.02D);
        }

        level.playSound(null, x, y, z, SoundEvents.GLOW_SQUID_SQUIRT, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 16, 1.0D, 0.6D, 1.0D, 0.03D);
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
