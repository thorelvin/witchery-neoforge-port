package com.emoniph.witchery.events;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles all werewolf gameplay: transformation buffs, wolfsbane weakness, and silver damage bonus.
 *
 * Wolf form buffs apply whenever the player is cursed (isWerewolf=true), not just at full moon.
 * The visual (wolfman model) also always shows — both are consistent.
 * Wolfsbane in inventory suppresses buffs and applies Weakness II.
 * Silver ingots/weapons deal bonus damage to werewolves and vampires.
 */
@EventBusSubscriber
public class WerewolfEvents {

    private static final int EFFECT_INTERVAL = 100; // ticks between effect refresh
    private static final int EFFECT_DURATION = 140;
    private static final int SYNC_INTERVAL   = 200;
    private static final float VILLAGER_INFECTION_CHANCE = 0.10F;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isWerewolf()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        int tick = player.tickCount;
        if (tick % EFFECT_INTERVAL == 0) applyWolfEffects(player, level, data);
        if (tick % 40              == 0) checkWolfsbane(player);
        if (data.getHowlCooldown() > 0) data.setHowlCooldown(data.getHowlCooldown() - 1);
        if (tick % SYNC_INTERVAL   == 0) PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void applyWolfEffects(ServerPlayer player, ServerLevel level, WitcheryPlayerData data) {
        if (!hasWolfsbane(player)) {
            if (!WitcheryConfig.WEREWOLF_FULL_MOON_ONLY.get() || isFullMoon(level)) {
                applyWolfForm(player, data);
            }
        }
    }

    /** Wolfsbane in inventory suppresses the wolf form and applies Weakness. */
    private static void checkWolfsbane(ServerPlayer player) {
        if (hasWolfsbane(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, true, true));
            // Remove wolf-form buffs
            player.removeEffect(MobEffects.SPEED);
            player.removeEffect(MobEffects.STRENGTH);
            player.removeEffect(MobEffects.JUMP_BOOST);
            if (player.tickCount % 200 == 0) {
                player.displayClientMessage(Component.translatable("witchery.werewolf.wolfsbane"), false);
            }
        }
    }

    private static boolean hasWolfsbane(ServerPlayer player) {
        return hasItemInInventory(player, WitcheryItems.WOLFSBANE_FLOWER.get());
    }

    private static boolean hasItemInInventory(ServerPlayer player, Item item) {
        for (int _i = 0; _i < player.getInventory().getContainerSize(); _i++) {
            ItemStack stack = player.getInventory().getItem(_i);
            if (stack.is(item)) return true;
        }
        return false;
    }

    // ── Silver damage bonus ───────────────────────────────────────────────────

    /** Silver held by attacker deals 2 bonus damage to werewolves and vampires. */
    @SubscribeEvent
    public static void onSilverDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (!isSilverHeld(attacker.getMainHandItem())) return;

        if (isWerewolfOrVampire(event.getEntity())) {
            event.setNewDamage(event.getNewDamage() + 2f);
        }
    }

    @SubscribeEvent
    public static void onWerewolfVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!(villager.level() instanceof ServerLevel level)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!WitcheryAttachments.get(player).isWerewolf()) return;
        if (!isFullMoonNight(level)) return;
        if (level.random.nextFloat() >= VILLAGER_INFECTION_CHANCE) return;

        if (spawnWereVillagerPlaceholder(level, villager, player)) {
            event.setCanceled(true);
            villager.discard();
        }
    }

    private static boolean isSilverHeld(ItemStack stack) {
        return stack.is(WitcheryItems.SILVER_INGOT.get())
                || stack.is(WitcheryItems.SILVER_SWORD.get());
    }

    private static boolean isWerewolfOrVampire(LivingEntity entity) {
        if (entity instanceof ServerPlayer target) {
            WitcheryPlayerData data = WitcheryAttachments.get(target);
            return data.isWerewolf() || data.isVampire();
        }
        return entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    private static boolean spawnWereVillagerPlaceholder(ServerLevel level, Villager villager, ServerPlayer target) {
        ZombieVillager zombie = EntityType.ZOMBIE_VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        if (zombie == null) {
            return false;
        }

        VillagerData villagerData = villager.getVillagerData();
        zombie.setVillagerData(villagerData);
        zombie.setPos(villager.getX(), villager.getY(), villager.getZ());
        zombie.setYRot(villager.getYRot());
        zombie.setXRot(villager.getXRot());
        zombie.setBaby(villager.isBaby());
        zombie.setPersistenceRequired();
        zombie.setTarget(target);
        zombie.getPersistentData().putBoolean("witchery_were_villager_placeholder", true);
        if (villager.hasCustomName()) {
            zombie.setCustomName(villager.getCustomName());
        }
        level.addFreshEntity(zombie);
        level.levelEvent(null, 1016, zombie.blockPosition(), 0);
        return true;
    }

    private static void applyWolfForm(ServerPlayer player, WitcheryPlayerData data) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, EFFECT_DURATION, 2, true, true));  // Speed III
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,   EFFECT_DURATION, 1, true, true));  // Strength II
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,           EFFECT_DURATION, 1, true, true));  // Jump Boost II
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER,         EFFECT_DURATION, 1, true, true));  // Hunger II
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,   EFFECT_DURATION, 0, true, false)); // Night Vision

        // Warn on first application each interval (not every tick)
        float prev = data.getTransformationLevel();
        if (prev < 1f) {
            data.setTransformationLevel(1f);
            player.displayClientMessage(Component.translatable("witchery.werewolf.transformed"), false);
        }
    }

    /** Phase 0 = full moon in vanilla's 8-phase moon cycle. */
    private static boolean isFullMoon(ServerLevel level) {
        return (level.getDayTime() / 24000L) % 8L == 0;
    }

    private static boolean isFullMoonNight(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        return isFullMoon(level) && dayTime > 13000L && dayTime < 23000L;
    }
}
