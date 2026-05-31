package com.emoniph.witchery.events;

import com.emoniph.witchery.item.GoblinClothesItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingGetProjectileEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.ArrowNockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public final class GoblinClothesEvents {
    private static final String MOGGED_ARROW_TAG = "WITCMogged";
    private static final double PAIRING_RANGE = 8.0D;

    private GoblinClothesEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 100 != 0) {
            return;
        }
        if (!GoblinClothesItem.isQuiverWorn(player) && !GoblinClothesItem.isGirdleWorn(player)) {
            return;
        }

        AABB bounds = player.getBoundingBox().inflate(PAIRING_RANGE);
        for (ServerPlayer other : player.level().getEntitiesOfClass(ServerPlayer.class, bounds,
                other -> other != player && other.isAlive())) {
            if ((GoblinClothesItem.isQuiverWorn(player) && GoblinClothesItem.isGirdleWorn(other))
                    || (GoblinClothesItem.isGirdleWorn(player) && GoblinClothesItem.isQuiverWorn(other))) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof AbstractArrow arrow
                && arrow.getPersistentData().getBooleanOr(MOGGED_ARROW_TAG, false)) {
            if (!target.onGround()) {
                event.setNewDamage(event.getNewDamage() * 3.0F);
            }
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            return;
        }

        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player) || direct != player) {
            return;
        }
        if (!GoblinClothesItem.isGirdleWorn(player) || !player.getMainHandItem().isEmpty()) {
            return;
        }

        event.setNewDamage(5.0F);
        target.push(0.0D, 1.0D, 0.0D);
        target.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onLivingGetProjectile(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof Player player) || !GoblinClothesItem.isQuiverWorn(player)) {
            return;
        }
        if (!(event.getProjectileWeaponItemStack().getItem() instanceof BowItem)) {
            return;
        }
        if (event.getProjectileItemStack().isEmpty()) {
            event.setProjectileItemStack(new ItemStack(Items.ARROW));
        }
    }

    @SubscribeEvent
    public static void onArrowNock(ArrowNockEvent event) {
        Player player = event.getEntity();
        if (event.hasAmmo() || !GoblinClothesItem.isQuiverWorn(player)) {
            return;
        }
        if (event.getBow().getItem() instanceof BowItem) {
            event.setAction(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        ItemStack bow = event.getBow();
        if (!(bow.getItem() instanceof BowItem) || !GoblinClothesItem.isQuiverWorn(player)) {
            return;
        }

        float power = BowItem.getPowerForTime(event.getCharge());
        if (power < 0.1F) {
            return;
        }

        event.setCanceled(true);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ((ArrowItem) Items.ARROW).createArrow(level, arrowStack, player, bow);
        arrow.getPersistentData().putBoolean(MOGGED_ARROW_TAG, true);
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
        if (power >= 1.0F) {
            arrow.setCritArrow(true);
        }

        level.addFreshEntity(arrow);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.0F / (level.random.nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        EquipmentSlot handSlot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? EquipmentSlot.OFFHAND
                : EquipmentSlot.MAINHAND;
        bow.hurtAndBreak(1, player, handSlot);
        player.awardStat(Stats.ITEM_USED.get(bow.getItem()));
    }
}
