package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.WitchHunterEntity;
import com.emoniph.witchery.events.PoppetEvents;
import com.emoniph.witchery.events.SilverEvents;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

import java.util.UUID;

/**
 * Voodoo Poppet — offensive. Must be bound to a player or mob via right-click.
 * Right-click (not sneaking): violently throws the bound entity with random velocity and brief levitation.
 * Sneak + right-click with Bone Needles: consumes one needle, deals 2 damage to the bound entity.
 * Hold submerged in water while bound: bound entity takes drowning damage every 20 ticks.
 * Use on lava block: sets bound entity on fire.
 * Each use costs durability. Breaks at 1000 uses.
 */
public class VoodooPoppetItem extends PoppetItem {
    private static final int MAX_USE_DURATION = 80;

    public VoodooPoppetItem(Properties props) { super(props); }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (!isBound(stack)) {
                user.displayClientMessage(Component.translatable("witchery.poppet.not_bound"), false);
                return InteractionResult.PASS;
            }
            if (SilverEvents.wearsFullHunterSet(user)) {
                user.displayClientMessage(Component.translatable("witchery.poppet.hunter_armor_blocked"), false);
                return InteractionResult.PASS;
            }

            LivingEntity target = resolveBoundEntity(stack, level);
            if (target == null || target == user) {
                user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.not_online"), false);
                return InteractionResult.PASS;
            }
        }
        return ItemUtils.startUsingInstantly(level, user, hand);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (level.isClientSide() || !(livingEntity instanceof Player user)) {
            return false;
        }
        if (!isBound(stack) || SilverEvents.wearsFullHunterSet(user)) {
            return false;
        }

        LivingEntity target = resolveBoundEntity(stack, level);
        if (target == null || target == user) {
            user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.not_online"), false);
            return false;
        }

        int elapsedTicks = Math.max(1, getUseDuration(stack, livingEntity) - timeLeft);
        return user.isShiftKeyDown()
                ? doNeedleStick(stack, user, target).consumesAction()
                : doViolentThrow(stack, user, target, elapsedTicks).consumesAction();
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        if (!level.getFluidState(pos).is(Fluids.LAVA) && !level.getFluidState(pos).is(Fluids.FLOWING_LAVA)) {
            return InteractionResult.PASS;
        }

        Player user = ctx.getPlayer();
        if (user == null) return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        if (!isBound(stack)) {
            user.displayClientMessage(Component.translatable("witchery.poppet.not_bound"), false);
            return InteractionResult.PASS;
        }
        if (SilverEvents.wearsFullHunterSet(user)) {
            user.displayClientMessage(Component.translatable("witchery.poppet.hunter_armor_blocked"), false);
            return InteractionResult.PASS;
        }

        LivingEntity target = resolveBoundEntity(stack, level);
        if (target == null || target == user) return InteractionResult.PASS;

        boolean blocked = target instanceof ServerPlayer targetPlayer
                && PoppetEvents.triggerVoodooProtection(targetPlayer, user, stack);
        if (!blocked) {
            target.igniteForSeconds(8.0f);
            user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.fire", getBoundName(stack)), false);
        }
        damagePoppet(stack, user, 1000);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 2.0F + level.random.nextFloat() * 0.4F);
        if (user instanceof ServerPlayer sp) WitchHunterEntity.blackMagicPerformed(sp);
        return InteractionResult.CONSUME;
    }

    private InteractionResult doViolentThrow(ItemStack stack, Player user, LivingEntity target, int elapsedTicks) {
        if (target instanceof ServerPlayer targetPlayer
                && PoppetEvents.triggerVoodooProtection(targetPlayer, user, stack)) {
            return InteractionResult.SUCCESS;
        }

        double scaling = Math.max(0.25D, Math.min(4.0D, elapsedTicks / 20.0D));
        var look = user.getLookAngle();
        double vx = look.x * 0.9D * scaling;
        double vy = look.y * 0.3D * scaling;
        double vz = look.z * 0.9D * scaling;
        target.setDeltaMovement(vx, vy, vz);
        if (target instanceof ServerPlayer sp) sp.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
        damagePoppet(stack, user, 10);
        user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.throw", getBoundName(stack)), false);
        if (user instanceof ServerPlayer sp) WitchHunterEntity.blackMagicPerformed(sp);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult doNeedleStick(ItemStack stack, Player user, LivingEntity target) {
        int needleSlot = -1;
        for (int i = 0; i < user.getInventory().getContainerSize(); i++) {
            if (user.getInventory().getItem(i).is(WitcheryItems.BONE_NEEDLE.get())) {
                needleSlot = i;
                break;
            }
        }
        if (needleSlot < 0) {
            user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.no_needles"), false);
            return InteractionResult.FAIL;
        }

        if (target instanceof ServerPlayer targetPlayer
                && PoppetEvents.triggerVoodooProtection(targetPlayer, user, stack)) {
            return InteractionResult.SUCCESS;
        }

        user.getInventory().getItem(needleSlot).shrink(1);
        target.hurt(((ServerLevel) target.level()).damageSources().magic(), 0.5f);
        damagePoppet(stack, user, 10);
        user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.needle", getBoundName(stack)), false);
        if (user instanceof ServerPlayer sp) WitchHunterEntity.blackMagicPerformed(sp);
        return InteractionResult.SUCCESS;
    }

    private void damagePoppet(ItemStack stack, Player user, int amount) {
        if (stack.isEmpty()) {
            return;
        }
        int current = getTag(stack).getIntOr("witchery_damage", 0) + amount;
        if (current >= 1000) {
            stack.shrink(1);
        } else {
            int dmg = current;
            net.minecraft.world.item.component.CustomData.update(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putInt("witchery_damage", dmg));
        }
    }

    /** Called from PoppetEvents tick: if the voodoo poppet holder is submerged, hurt the bound entity. */
    public static void tickUnderwaterEffect(ServerPlayer holder, ItemStack stack) {
        if (!PoppetItem.isBound(stack)) return;
        if (SilverEvents.wearsFullHunterSet(holder)) return;
        if (!holder.isUnderWater()) return;
        if (holder.tickCount % 20 != 0) return;

        LivingEntity target = resolveBoundEntity(stack, holder.level());
        if (target == null || target == holder) return;
        if (target instanceof ServerPlayer targetPlayer
                && PoppetEvents.triggerVoodooProtection(targetPlayer, holder, stack)) {
            return;
        }

        target.hurt(((ServerLevel) target.level()).damageSources().drown(), 2.0f);
        WitchHunterEntity.blackMagicPerformed(holder);
    }

    private static LivingEntity resolveBoundEntity(ItemStack stack, Level level) {
        UUID uuid = getBoundUUID(stack).orElse(null);
        if (uuid == null) return null;

        ServerLevel serverLevel = (ServerLevel) level;
        if (!isMobBound(stack)) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player;
            }
        }

        return PoppetItem.findLivingEntity(serverLevel.getServer(), uuid).orElse(null);
    }
}
