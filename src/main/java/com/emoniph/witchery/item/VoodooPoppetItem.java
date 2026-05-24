package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.WitchHunterEntity;
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

    public VoodooPoppetItem(Properties props) { super(props); }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS.heldItemTransformedTo(stack);

        if (!isBound(stack)) {
            user.displayClientMessage(Component.translatable("witchery.poppet.not_bound"), false);
            return InteractionResult.PASS;
        }

        LivingEntity target = resolveBoundEntity(stack, level);
        if (target == null || target == user) {
            user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.not_online"), false);
            return InteractionResult.PASS;
        }

        if (user.isShiftKeyDown()) {
            return doNeedleStick(stack, user, target);
        } else {
            return doViolentThrow(stack, user, target, level);
        }
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

        LivingEntity target = resolveBoundEntity(stack, level);
        if (target == null || target == user) return InteractionResult.PASS;

        target.igniteForSeconds(8.0f);
        damagePoppet(stack, user, 10);
        user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.fire", getBoundName(stack)), false);
        if (user instanceof ServerPlayer sp) WitchHunterEntity.blackMagicPerformed(sp);
        return InteractionResult.CONSUME;
    }

    private InteractionResult doViolentThrow(ItemStack stack, Player user, LivingEntity target, Level level) {
        var random = level.getRandom();
        double vx = (random.nextDouble() - 0.5) * 2.0;
        double vy = 0.5 + random.nextDouble() * 0.5;
        double vz = (random.nextDouble() - 0.5) * 2.0;
        target.setDeltaMovement(vx, vy, vz);
        if (target instanceof ServerPlayer sp) sp.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
        damagePoppet(stack, user, 5);
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
        user.getInventory().getItem(needleSlot).shrink(1);
        target.hurt(((ServerLevel) target.level()).damageSources().magic(), 2.0f);
        damagePoppet(stack, user, 2);
        user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.needle", getBoundName(stack)), false);
        if (user instanceof ServerPlayer sp) WitchHunterEntity.blackMagicPerformed(sp);
        return InteractionResult.SUCCESS;
    }

    private void damagePoppet(ItemStack stack, Player user, int amount) {
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
        if (!holder.isUnderWater()) return;
        if (holder.tickCount % 20 != 0) return;

        LivingEntity target = resolveBoundEntity(stack, holder.level());
        if (target == null || target == holder) return;

        target.hurt(((ServerLevel) target.level()).damageSources().drown(), 2.0f);
        WitchHunterEntity.blackMagicPerformed(holder);
    }

    private static LivingEntity resolveBoundEntity(ItemStack stack, Level level) {
        UUID uuid = getBoundUUID(stack).orElse(null);
        if (uuid == null) return null;

        if (!isMobBound(stack)) {
            return ((ServerLevel) level).getServer().getPlayerList().getPlayer(uuid);
        }

        Entity entity = ((ServerLevel) level).getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }
}
