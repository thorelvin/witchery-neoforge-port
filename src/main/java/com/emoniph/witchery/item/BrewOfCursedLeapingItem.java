package com.emoniph.witchery.item;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Brew of Cursed Leaping - source-style jump burst in practical item form. */
public class BrewOfCursedLeapingItem extends BrewItem {
    private static final double RADIUS = 5.0D;

    public BrewOfCursedLeapingItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos center = blockImpactPosition(serverLevel, context.getClickedPos(), context.getClickedFace());
        explodeLeaping(serverLevel, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, null);
        consumeBrew(context.getPlayer(), context.getHand(), context.getItemInHand());
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

        explodeLeaping(serverLevel, target.getX(), target.getY(), target.getZ(), target);
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            explodeLeaping(serverLevel, player.getX(), player.getY(), player.getZ(), player);
        }
    }

    private static BlockPos blockImpactPosition(ServerLevel level, BlockPos clickedPos, Direction face) {
        BlockPos pos = clickedPos.relative(face);
        if (face == Direction.UP && !level.getBlockState(clickedPos).isSolid()) {
            return pos.below();
        }
        return pos;
    }

    private static void explodeLeaping(ServerLevel level, double x, double y, double z, LivingEntity directTarget) {
        double radiusSq = RADIUS * RADIUS;
        AABB area = new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D)
                .inflate(RADIUS, 2.0D, RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double distanceSq = target.distanceToSqr(x, y, z);
            if (distanceSq >= radiusSq) {
                continue;
            }

            double strength = 1.0D - 0.25D * Math.sqrt(distanceSq) / RADIUS;
            if (target == directTarget) {
                strength = 1.0D;
            }
            double leap = strength * 1.6D;
            Vec3 velocity = target.getDeltaMovement();
            target.setDeltaMovement(velocity.x, velocity.y + leap, velocity.z);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 3));

            level.sendParticles(ParticleTypes.POOF,
                    target.getX(), target.getY() + target.getBbHeight() * 0.15D, target.getZ(),
                    8, target.getBbWidth() * 0.35D, 0.2D, target.getBbWidth() * 0.35D, 0.02D);
        }

        level.playSound(null, x, y, z, SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.9F, 0.8F);
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
