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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Brew of Revealing - exposes nearby invisible or hidden living entities. */
public class BrewOfRevealingItem extends BrewItem {
    private static final double NORMAL_RADIUS = 5.0D;
    private static final double ENHANCED_RADIUS = 8.0D;

    public BrewOfRevealingItem(Properties properties) { super(properties); }

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
        revealNearby(serverLevel, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                hasBrewMastery(context.getPlayer()));
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

        revealNearby(serverLevel, target.getX(), target.getY(), target.getZ(), hasBrewMastery(player));
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            revealNearby(serverLevel, player.getX(), player.getY(), player.getZ(), hasBrewMastery(player));
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

    private static void revealNearby(ServerLevel level, double x, double y, double z, boolean enhanced) {
        double radius = enhanced ? ENHANCED_RADIUS : NORMAL_RADIUS;
        double radiusSq = radius * radius;
        AABB area = new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D)
                .inflate(radius);
        int revealed = 0;

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity.distanceToSqr(x, y, z) > radiusSq) {
                continue;
            }
            if (revealEntity(level, entity)) {
                revealed++;
            }
        }

        level.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.4F);
        level.sendParticles(ParticleTypes.SMOKE, x, y + 0.5D, z,
                Math.max(8, revealed * 8), radius * 0.25D, 0.35D, radius * 0.25D, 0.01D);
    }

    private static boolean revealEntity(ServerLevel level, LivingEntity entity) {
        boolean hadInvisibility = entity.hasEffect(MobEffects.INVISIBILITY);
        boolean wasInvisible = entity.isInvisible();
        if (hadInvisibility) {
            entity.removeEffect(MobEffects.INVISIBILITY);
        }
        if (wasInvisible) {
            entity.setInvisible(false);
        }

        if (hadInvisibility || wasInvisible) {
            level.sendParticles(ParticleTypes.SMOKE,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                    20, entity.getBbWidth() * 0.5D, entity.getBbHeight() * 0.5D,
                    entity.getBbWidth() * 0.5D, 0.02D);
            return true;
        }
        return false;
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
