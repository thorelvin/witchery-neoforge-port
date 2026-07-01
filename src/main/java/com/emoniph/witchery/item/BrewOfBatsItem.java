package com.emoniph.witchery.item;

import com.emoniph.witchery.brewing.Brews;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Brew of Bats - source-style no-drop bat burst and short high Slowness hit. */
public class BrewOfBatsItem extends BrewItem {
    private static final int BAT_COUNT = 10;
    private static final double RADIUS = 3.0D;
    private static final String NO_DROPS_TAG = "WITCNoDrops";

    public BrewOfBatsItem(Properties p) { super(p); }

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
        burstBats(serverLevel, center);
        slowNearby(serverLevel, center, null);
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

        BlockPos center = target.blockPosition();
        burstBats(serverLevel, center);
        slowNearby(serverLevel, center, target);
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = player.blockPosition();
        burstBats(serverLevel, center);
        slowNearby(serverLevel, center, null);
    }

    private static BlockPos blockImpactPosition(ServerLevel level, BlockPos clickedPos, Direction face) {
        BlockPos pos = clickedPos.relative(face);
        if (face == Direction.UP && !level.getBlockState(clickedPos).isSolid()) {
            return pos.below();
        }
        return pos;
    }

    private static void burstBats(ServerLevel level, BlockPos center) {
        for (int i = 0; i < BAT_COUNT; i++) {
            Bat bat = EntityType.BAT.create(level, EntitySpawnReason.TRIGGERED);
            if (bat == null) {
                continue;
            }
            bat.getPersistentData().putBoolean(NO_DROPS_TAG, true);
            bat.getPersistentData().putBoolean(Brews.SUMMONED_NO_DROPS_TAG, true);
            bat.setPos(center.getX(), center.getY(), center.getZ());
            level.addFreshEntity(bat);
        }

        level.playSound(null, center, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.EXPLOSION,
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                16, 1.5D, 1.5D, 1.5D, 0.0D);
    }

    private static void slowNearby(ServerLevel level, BlockPos center, LivingEntity directTarget) {
        AABB area = new AABB(center).inflate(RADIUS, 2.0D, RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) < RADIUS * RADIUS)) {
            double distance = Math.sqrt(target.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D));
            double strength = 1.0D - distance / RADIUS;
            if (target == directTarget) {
                strength = 1.0D;
            }
            int duration = Math.max(1, (int)(strength * 100.0D + 0.5D));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 5));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
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
