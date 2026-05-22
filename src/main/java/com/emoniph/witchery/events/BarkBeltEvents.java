package com.emoniph.witchery.events;

import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.BarkBeltItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public final class BarkBeltEvents {
    private BarkBeltEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 100 != 0) {
            return;
        }

        ItemStack belt = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!belt.is(WitcheryItems.BARK_BELT.get())) {
            return;
        }
        if (!isRechargeBlockBelow(player)) {
            return;
        }

        int max = BarkBeltItem.getMaxChargeLevel(player);
        int current = BarkBeltItem.getChargeLevel(belt);
        if (current >= max) {
            return;
        }

        BarkBeltItem.setChargeLevel(belt, Math.min(current + 1, max));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WOOD_PLACE,
                SoundSource.PLAYERS, 0.35F, 0.8F + player.getRandom().nextFloat() * 0.3F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getNewDamage() <= 0.0F || event.getSource().getEntity() == null) {
            return;
        }

        ItemStack belt = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!belt.is(WitcheryItems.BARK_BELT.get()) || isWoodenDamage(event.getSource())) {
            return;
        }

        int current = Math.min(BarkBeltItem.getChargeLevel(belt), BarkBeltItem.getMaxChargeLevel(player));
        if (current <= 0) {
            return;
        }

        int cost = current > 1 && player.getRandom().nextDouble() < 0.25 ? 2 : 1;
        BarkBeltItem.setChargeLevel(belt, Math.max(current - cost, 0));
        event.setNewDamage(0.0F);
        dropSpentBark(player, cost);
    }

    private static boolean isRechargeBlockBelow(ServerPlayer player) {
        return player.level().getBlockState(player.blockPosition().below()).is(Blocks.GRASS_BLOCK)
                || player.level().getBlockState(player.blockPosition().below()).is(Blocks.MYCELIUM);
    }

    private static boolean isWoodenDamage(DamageSource source) {
        if (source.getEntity() instanceof HornedHuntsmanEntity && !source.is(DamageTypeTags.IS_PROJECTILE)) {
            return true;
        }
        if (source.getEntity() instanceof LivingEntity living) {
            return isWoodenWeapon(living.getMainHandItem()) || isWoodenWeapon(living.getOffhandItem());
        }
        return false;
    }

    private static boolean isWoodenWeapon(ItemStack stack) {
        return stack.is(Items.WOODEN_SWORD) || stack.is(WitcheryItems.LEGACY_PLACEHOLDER_ITEMS.get("stake").get());
    }

    private static void dropSpentBark(ServerPlayer player, int cost) {
        for (int i = 0; i < cost; i++) {
            double dx = player.getRandom().nextBoolean() ? -1.0 : 1.0;
            double dz = player.getRandom().nextBoolean() ? -1.0 : 1.0;
            ItemEntity item = new ItemEntity(player.level(), player.getX() + dx, player.getY() + 1.5,
                    player.getZ() + dz, new ItemStack(Items.STICK));
            item.setPickUpDelay(60);
            item.lifespan = 60;
            player.level().addFreshEntity(item);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.WOOD_HIT,
                SoundSource.PLAYERS, 0.45F, 0.8F + player.getRandom().nextFloat() * 0.2F);
    }
}
