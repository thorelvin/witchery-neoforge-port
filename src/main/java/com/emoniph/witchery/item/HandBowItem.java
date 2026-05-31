package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.BoltEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Witch-Hunter pistol crossbow.
 *
 * Source parity: ItemHandBow 1.7.10-0.24.1.
 * Current practical slice fires all promoted source bolt families:
 * wooden/stake, nullifying, bone/holy, splitting, and silver.
 */
public class HandBowItem extends Item {

    private static final int MAX_DRAW_TICKS = 20;
    private static final float BOLT_SPEED = 3.0F;
    private static final float BOLT_DAMAGE = 6.0F;
    private static final float SPLITTING_BOLT_DAMAGE = 1.0F;

    public HandBowItem(Properties properties) {
        super(properties.durability(768));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (findBolt(player) == null) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        int drawnTicks = getUseDuration(stack, entity) - timeLeft;
        float charge = Math.min(drawnTicks, MAX_DRAW_TICKS) / (float) MAX_DRAW_TICKS;
        if (charge < 0.1F) return false;

        ItemStack boltStack = findBolt(player);
        if (boltStack == null) return false;

        if (!level.isClientSide()) {
            Vec3 look = player.getLookAngle();
            if (boltStack.is(WitcheryItems.SPLITTING_BOLT.get())) {
                launchBolt(level, player, BoltEntity.WOODEN, charge, SPLITTING_BOLT_DAMAGE, look.yRot((float) Math.toRadians(-20.0D)));
                launchBolt(level, player, BoltEntity.WOODEN, charge, SPLITTING_BOLT_DAMAGE, look);
                launchBolt(level, player, BoltEntity.WOODEN, charge, SPLITTING_BOLT_DAMAGE, look.yRot((float) Math.toRadians(20.0D)));
            } else {
                launchBolt(level, player, boltTypeFor(boltStack), charge, BOLT_DAMAGE, look);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);

            if (!player.getAbilities().instabuild) {
                boltStack.shrink(1);
            }

            stack.hurtAndBreak(2, player, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    private static void launchBolt(Level level, Player player, int boltType, float charge, float damage, Vec3 direction) {
        BoltEntity bolt = new BoltEntity(level, player, boltType);
        bolt.setBaseDamage(damage * charge);
        bolt.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        bolt.shoot(direction.x, direction.y, direction.z, BOLT_SPEED * charge, 1.0F);
        bolt.pickup = AbstractArrow.Pickup.ALLOWED;
        level.addFreshEntity(bolt);
    }

    private static int boltTypeFor(ItemStack stack) {
        if (stack.is(WitcheryItems.SILVER_BOLT.get())) return BoltEntity.SILVER;
        if (stack.is(WitcheryItems.NULLIFYING_BOLT.get())) return BoltEntity.NULLIFYING;
        if (stack.is(WitcheryItems.BONE_BOLT.get())) return BoltEntity.BONE;
        return BoltEntity.WOODEN;
    }

    private static ItemStack findBolt(Player player) {
        ItemStack stack = findBolt(player, WitcheryItems.SILVER_BOLT.get());
        if (stack != null) return stack;
        stack = findBolt(player, WitcheryItems.NULLIFYING_BOLT.get());
        if (stack != null) return stack;
        stack = findBolt(player, WitcheryItems.BONE_BOLT.get());
        if (stack != null) return stack;
        stack = findBolt(player, WitcheryItems.SPLITTING_BOLT.get());
        if (stack != null) return stack;
        return findBolt(player, WitcheryItems.WOODEN_BOLT.get());
    }

    private static ItemStack findBolt(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) return stack;
        }
        return null;
    }
}
