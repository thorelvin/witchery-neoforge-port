package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.BoltEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Witch-Hunter crossbow pistol.
 * Fires BoltEntity projectiles (silver > wooden, priority order).
 * Uses bow-style draw animation; fires on release at full charge (20 ticks).
 *
 * Source parity: ItemHandBow 1.7.10-0.24.1
 *   - fires silver bolt if available, else wooden bolt
 *   - Infinity enchantment has 25% chance to not consume the bolt
 */
public class HandBowItem extends Item {

    private static final int MAX_DRAW_TICKS = 20;
    private static final float BOLT_SPEED   = 3.0F;
    private static final float BOLT_DAMAGE  = 6.0F;

    public HandBowItem(Properties properties) {
        super(properties.durability(768));
    }

    // ── Use — starts drawing ──────────────────────────────────────────────────

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
        return 72000; // let releaseUsing handle timing
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    // ── Release — fires the bolt ──────────────────────────────────────────────

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        int drawnTicks = getUseDuration(stack, entity) - timeLeft;
        float charge   = Math.min(drawnTicks, MAX_DRAW_TICKS) / (float) MAX_DRAW_TICKS;
        if (charge < 0.1f) return false;

        ItemStack boltStack = findBolt(player);
        if (boltStack == null) return false;

        if (!level.isClientSide()) {
            int boltType = boltStack.is(WitcheryItems.SILVER_BOLT.get())
                    ? BoltEntity.SILVER : BoltEntity.WOODEN;

            BoltEntity bolt = new BoltEntity(level, player, boltType);
            bolt.setBaseDamage(BOLT_DAMAGE * charge);
            bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());

            // Aim using player look direction
            Vec3 look = player.getLookAngle();
            bolt.shoot(look.x, look.y, look.z, BOLT_SPEED * charge, 1.0F);
            bolt.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED;

            level.addFreshEntity(bolt);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);

            if (!player.getAbilities().instabuild) {
                boltStack.shrink(1);
            }

            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        return true;
    }

    // ── Helper — find bolt in inventory ───────────────────────────────────────

    private static ItemStack findBolt(Player player) {
        // Prefer silver, fall back to wooden
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(WitcheryItems.SILVER_BOLT.get())) return s;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(WitcheryItems.WOODEN_BOLT.get())) return s;
        }
        return null;
    }
}
