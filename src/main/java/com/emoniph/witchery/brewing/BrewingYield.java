package com.emoniph.witchery.brewing;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/** Source-faithful custom-brew bottling skill and yield calculation. */
public final class BrewingYield {

    public static final int FULL_CAULDRON_UNITS = 3000;
    public static final int MAX_SKILL = 100;
    private static final int[] DRAIN_UNITS = {3000, 1500, 1000, 750, 600, 500, 375, 300, 200, 100};

    private BrewingYield() {}

    public static int drainAmount(Player player, List<Item> ingredients) {
        MobEffectInstance expertise = player.getEffect(WitcheryMobEffects.BREWING_EXPERTISE);
        int potionLevel = expertise == null ? 0 : expertise.getAmplifier() + 1;
        return drainAmount(
                WitcheryAttachments.getBrewingSkill(player),
                potionLevel,
                gearLevel(player),
                FamiliarHelper.hasActiveOwlFamiliar(player),
                yieldPenalty(ingredients));
    }

    public static int drainAmount(int skill, int potionLevel, int gearLevel,
                                  boolean hasBrewMasteryFamiliar, int yieldPenalty) {
        int tier = baseYieldTier(skill, potionLevel, gearLevel, hasBrewMasteryFamiliar);
        return DRAIN_UNITS[Math.clamp(tier - Math.max(0, yieldPenalty), 0, DRAIN_UNITS.length - 1)];
    }

    public static int bottlesPerCauldron(int drainAmount) {
        return drainAmount <= 0 ? 0 : FULL_CAULDRON_UNITS / drainAmount;
    }

    static int baseYieldTier(int skill, int potionLevel, int gearLevel, boolean hasBrewMasteryFamiliar) {
        int skillTier = Math.clamp(skill, 0, MAX_SKILL) / 30;
        int potion = Math.max(0, potionLevel);
        int gear = Math.max(0, gearLevel);
        if (skillTier == 0) {
            return 0;
        }
        if (skillTier == 1) {
            return gear < 1 && potion < 1 ? 1 : 2;
        }
        if (gear >= 3 && potion >= 3 && hasBrewMasteryFamiliar) {
            return 9;
        }
        if (gear >= 2 && potion >= 3 && hasBrewMasteryFamiliar) {
            return 8;
        }
        if (gear >= 2 && potion >= 3) {
            return 7;
        }
        if (gear >= 2 && potion >= 2) {
            return 6;
        }
        if (gear >= 2 && potion >= 1) {
            return 5;
        }
        if (gear >= 2 || potion >= 1) {
            return 4;
        }
        return gear < 1 && potion < 1 ? 2 : 3;
    }

    public static boolean canIncreaseSkill(int currentSkill, int availableCapacity, int usedCapacity) {
        if (usedCapacity > availableCapacity || availableCapacity == 0 || currentSkill >= MAX_SKILL) {
            return false;
        }
        if (currentSkill < 30) {
            return availableCapacity > 1 && usedCapacity > 1;
        }
        if (currentSkill < 60) {
            return availableCapacity >= 4 && usedCapacity >= 4;
        }
        if (currentSkill < 90) {
            return availableCapacity >= 6 && usedCapacity >= 6;
        }
        return availableCapacity >= 8 && usedCapacity >= 8;
    }

    public static void awardBottlingSkill(Player player, List<Item> ingredients) {
        int current = WitcheryAttachments.getBrewingSkill(player);
        if (!canIncreaseSkill(current, Brews.availableCapacity(ingredients), Brews.usedCapacity(ingredients))) {
            return;
        }
        int next = Math.min(MAX_SKILL, current + 1);
        WitcheryAttachments.setBrewingSkill(player, next);
        if (next == 30 || next == 60 || next == 90) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.brew.skill_increase", next), false);
        }
    }

    public static int yieldPenalty(List<Item> ingredients) {
        return ingredients.contains(Items.DIAMOND) ? 2 : 0;
    }

    static int gearLevel(Player player) {
        int level = 0;
        Item head = player.getItemBySlot(EquipmentSlot.HEAD).getItem();
        Item chest = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        if (head == WitcheryItems.WITCHES_HAT.get()) {
            level += 1;
        }
        if (head == WitcheryItems.BABAS_HAT.get()) {
            level += 2;
        }
        if (chest == WitcheryItems.WITCHES_ROBES.get()) {
            level += 1;
        }
        if (chest == WitcheryItems.NECROMANCER_ROBES.get()) {
            level += 1;
        }
        return level;
    }
}
