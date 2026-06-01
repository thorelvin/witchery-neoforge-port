package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.LilithEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.CaneSwordItem;
import com.emoniph.witchery.item.ImmortalsObservationsBookItem;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitcheryMobDropEvents {
    private static final float SOURCE_RARE_DROP_CHANCE = 0.33f;
    private static final float SOURCE_ARTHANA_RARE_DROP_CHANCE = 0.5f;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        var rng = entity.getRandom();
        var pos = entity.blockPosition();
        var level = entity.level();
        Player killer = event.getSource().getEntity() instanceof Player player ? player : null;
        if (killer instanceof ServerPlayer serverPlayer) {
            CaneSwordItem.tryAbsorbBloodFromKill(serverPlayer, entity);
        }
        boolean arthana = killer != null && killer.getMainHandItem().is(WitcheryItems.ARTHANA.get());

        if (killer != null
                && hasIncompleteObservationsBook(killer)
                && shouldDropTornPage(entity, rng.nextFloat())
                && !hasDrop(event, WitcheryItems.TORN_PAGE.get())) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY() + 1.0, pos.getZ(),
                    new ItemStack(WitcheryItems.TORN_PAGE.get())));
        }

        if (entity instanceof Bat && rng.nextFloat() < (arthana ? 0.75f : 0.33f)) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.BAT_WOOL.get())));
        }

        if (entity instanceof Wolf && rng.nextFloat() < (arthana ? 0.75f : 0.33f)) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.TONGUE_OF_DOG.get())));
        }

        if (entity instanceof Sheep sheep
                && !sheep.isBaby()
                && isSourceWerewolf(event.getSource().getEntity())
                && rng.nextInt(4) != 0) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.SHEEP_LIVER.get())));
        }

        if (entity instanceof Creeper) {
            float chance = Mth.clamp(arthana ? 0.08f : 0.02f, 0.0f, 1.0f);
            if (rng.nextFloat() < chance) {
                event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                        new ItemStack(WitcheryItems.CREEPER_HEART.get())));
            }
        }

        if (entity instanceof OwlFamiliarEntity
                && rng.nextFloat() < sourceArthanaChance(arthana)) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.OWLETS_WING.get())));
        }

        if (entity instanceof ToadFamiliarEntity
                && rng.nextFloat() < sourceArthanaChance(arthana)) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.TOE_OF_FROG.get())));
        }

        if (entity instanceof Witch && rng.nextFloat() < 0.4f) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.REEK_OF_MISFORTUNE.get())));
        }

        if ((entity instanceof Witch || entity instanceof CovenWitchEntity)
                && killer != null
                && rng.nextFloat() < sourceArthanaChance(arthana)
                && !hasDrop(event, WitcheryItems.WITCH_HAND.get())) {
            event.getDrops().add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(WitcheryItems.WITCH_HAND.get())));
        }
    }

    private static boolean hasIncompleteObservationsBook(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (ImmortalsObservationsBookItem.canAddTornPage(inventory.getItem(slot))) {
                return true;
            }
        }
        return ImmortalsObservationsBookItem.canAddTornPage(inventory.getItem(Inventory.SLOT_OFFHAND));
    }

    private static boolean shouldDropTornPage(LivingEntity entity, float roll) {
        if (isAlwaysTornPageTarget(entity)) {
            return true;
        }
        if (entity.getType() == EntityType.ENDERMAN || entity.getType() == EntityType.ZOMBIFIED_PIGLIN) {
            return roll < 0.09f;
        }
        if (entity instanceof AbstractVillager) {
            return roll < 0.10f;
        }
        return entity.getType().is(EntityTypeTags.UNDEAD) && roll < 0.02f;
    }

    private static float sourceArthanaChance(boolean arthana) {
        return arthana ? SOURCE_ARTHANA_RARE_DROP_CHANCE : SOURCE_RARE_DROP_CHANCE;
    }

    private static boolean isSourceWerewolf(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            var data = WitcheryAttachments.get(player);
            return data.isWerewolf() && WerewolfShiftUtil.isTransformed(data);
        }
        return entity instanceof WolfmanEntity;
    }

    private static boolean isAlwaysTornPageTarget(LivingEntity entity) {
        return entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof BabaYagaEntity
                || entity instanceof DeathEntity
                || entity instanceof GoblinGulgEntity
                || entity instanceof GoblinMogEntity
                || entity instanceof HornedHuntsmanEntity
                || entity instanceof LeonardEntity
                || entity instanceof LilithEntity
                || entity instanceof LordOfTormentEntity
                || entity instanceof ReflectionEntity;
    }

    private static boolean hasDrop(LivingDropsEvent event, Item item) {
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(item)) {
                return true;
            }
        }
        return false;
    }
}
