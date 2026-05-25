package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.HornedHuntsmanEntity;
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
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Brew of Frog's Tongue - source-style pull toward the thrower in practical item form. */
public class BrewOfFrogsTongueItem extends BrewItem {
    private static final double RADIUS = 4.0D;
    private static final double PULL_SCALE = 0.15D;
    private static final double PULL_Y = 0.4D;

    public BrewOfFrogsTongueItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos center = blockImpactPosition(serverLevel, context.getClickedPos(), context.getClickedFace());
        pullNearby(serverLevel, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, player);
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

        pullNearby(serverLevel, target.getX(), target.getY(), target.getZ(), player);
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            pullNearby(serverLevel, player.getX(), player.getY(), player.getZ(), player);
        }
    }

    private static BlockPos blockImpactPosition(ServerLevel level, BlockPos clickedPos, Direction face) {
        BlockPos pos = clickedPos.relative(face);
        if (face == Direction.UP && !level.getBlockState(clickedPos).isSolid()) {
            return pos.below();
        }
        return pos;
    }

    private static void pullNearby(ServerLevel level, double x, double y, double z, Player thrower) {
        double radiusSq = RADIUS * RADIUS;
        AABB area = new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D)
                .inflate(RADIUS, 2.0D, RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == thrower
                    || target instanceof EnderDragon
                    || target instanceof HornedHuntsmanEntity
                    || target.distanceToSqr(x, y, z) >= radiusSq) {
                continue;
            }

            pullTarget(level, target, thrower);
        }

        level.playSound(null, x, y, z, SoundEvents.FROG_TONGUE, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 10, 0.7D, 0.3D, 0.7D, 0.03D);
    }

    private static void pullTarget(ServerLevel level, LivingEntity target, Player thrower) {
        Vec3 delta = thrower.position().subtract(target.position());
        target.setDeltaMovement(delta.x * PULL_SCALE, PULL_Y, delta.z * PULL_SCALE);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20, 1));
        level.sendParticles(ParticleTypes.ITEM_SLIME,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                6, target.getBbWidth() * 0.35D, target.getBbHeight() * 0.25D,
                target.getBbWidth() * 0.35D, 0.02D);
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
        player.setItemInHand(hand, stack);
    }
}
