package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.util.Enslaved;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Necromantic Stone source command surface.
 *
 * <p>Normal use orders nearby undead thralls. Sneak-use while an Infused Brew of the Grave is active
 * binds an undead target, respecting the old 200-tick cooldown gate.</p>
 */
public class NecroStoneItem extends Item {
    public static final int GRAVE_USE_COOLDOWN_TICKS = 200;
    private static final double TARGET_RANGE = 15.0D;
    private static final double MINION_HORIZONTAL_RANGE = 50.0D;
    private static final double MINION_VERTICAL_RANGE = 15.0D;

    public NecroStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        HitResult hit = pickEntityOrBlock(serverPlayer, TARGET_RANGE);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            return handleLivingTarget(serverPlayer, target);
        }
        if (hit instanceof BlockHitResult blockHit) {
            return handleBlockTarget(serverPlayer, blockHit);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        return handleBlockTarget(player, new BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return handleLivingTarget(serverPlayer, target);
    }

    private static InteractionResult handleLivingTarget(ServerPlayer player, LivingEntity target) {
        if (player.isShiftKeyDown()) {
            return tryBindGraveTarget(player, target) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        int ordered = orderThrallsToAttack(player, target);
        if (ordered <= 0) {
            failFeedback(player, target.position());
            return InteractionResult.FAIL;
        }
        successFeedback((ServerLevel) player.level(), target.position(), true);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleBlockTarget(ServerPlayer player, BlockHitResult hit) {
        BlockPos destination = necroCommandDestination(hit);
        if (destination == null) {
            return InteractionResult.PASS;
        }
        int ordered = orderThrallsToMove(player, destination);
        if (ordered <= 0) {
            failFeedback(player, Vec3.atCenterOf(destination));
            return InteractionResult.FAIL;
        }
        successFeedback((ServerLevel) player.level(), Vec3.atCenterOf(destination), false);
        return InteractionResult.SUCCESS;
    }

    private static boolean tryBindGraveTarget(ServerPlayer player, LivingEntity target) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        long now = player.level().getGameTime();
        boolean canBind = canUseGraveBinding(
                data.hasGraveBrew(),
                graveCooldownReady(now, data.getLastGraveUseGameTime()),
                isUndead(target),
                target instanceof Mob mob && Enslaved.canBeEnslaved(mob),
                target instanceof Mob mob && Enslaved.isEnslavedBy(mob, player));
        if (!canBind || !(target instanceof Mob mob) || !Enslaved.enslave(mob, player)) {
            failFeedback(player, target.position());
            return false;
        }
        data.setLastGraveUseGameTime(now);
        successFeedback((ServerLevel) player.level(), target.position(), false);
        player.displayClientMessage(Component.translatable("witchery.necro_stone.grave.bound",
                target.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    public static boolean graveCooldownReady(long now, long lastUse) {
        return lastUse == Long.MIN_VALUE || now - lastUse > GRAVE_USE_COOLDOWN_TICKS;
    }

    public static boolean canUseGraveBinding(boolean active, boolean cooldownReady, boolean undead,
                                             boolean canBeEnslaved, boolean alreadyOwned) {
        return active && cooldownReady && undead && canBeEnslaved && !alreadyOwned;
    }

    static BlockPos necroCommandDestination(BlockHitResult hit) {
        if (hit == null || hit.getType() == HitResult.Type.MISS || hit.getDirection() != Direction.UP) {
            return null;
        }
        return hit.getBlockPos().above();
    }

    private static int orderThrallsToAttack(ServerPlayer player, LivingEntity target) {
        int count = 0;
        for (Mob mob : nearbyOwnedUndead(player)) {
            if (mob == target) {
                continue;
            }
            mob.getNavigation().stop();
            mob.setTarget(target);
            count++;
        }
        return count;
    }

    private static int orderThrallsToMove(ServerPlayer player, BlockPos destination) {
        int count = 0;
        for (Mob mob : nearbyOwnedUndead(player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
                    1.0D);
            count++;
        }
        return count;
    }

    private static Iterable<Mob> nearbyOwnedUndead(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(MINION_HORIZONTAL_RANGE, MINION_VERTICAL_RANGE,
                MINION_HORIZONTAL_RANGE);
        return player.level().getEntitiesOfClass(Mob.class, area,
                mob -> isUndead(mob) && Enslaved.isEnslavedBy(mob, player));
    }

    private static boolean isUndead(LivingEntity entity) {
        return entity != null && entity.getType().is(EntityTypeTags.UNDEAD);
    }

    private static HitResult pickEntityOrBlock(ServerPlayer player, double range) {
        HitResult blockHit = player.pick(range, 0.0F, false);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        double maxDistanceSq = blockHit.getType() == HitResult.Type.MISS
                ? range * range
                : blockHit.getLocation().distanceToSqr(eye);
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, search, NecroStoneItem::canRaytraceTarget, maxDistanceSq);
        return entityHit != null ? entityHit : blockHit;
    }

    private static boolean canRaytraceTarget(Entity entity) {
        return entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator();
    }

    private static void successFeedback(ServerLevel level, Vec3 pos, boolean attackCommand) {
        level.sendParticles(attackCommand ? ParticleTypes.CRIT : ParticleTypes.WITCH,
                pos.x, pos.y + 0.7D, pos.z, 16, 0.45D, 0.45D, 0.45D, 0.02D);
        level.playSound(null, pos.x, pos.y, pos.z,
                attackCommand ? SoundEvents.ZOMBIE_DEATH : SoundEvents.ZOMBIE_INFECT,
                SoundSource.PLAYERS, 0.5F, attackCommand ? 0.8F : 1.0F);
    }

    private static void failFeedback(ServerPlayer player, Vec3 pos) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.7D, pos.z,
                10, 0.35D, 0.35D, 0.35D, 0.01D);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.5F, 0.7F);
    }
}
