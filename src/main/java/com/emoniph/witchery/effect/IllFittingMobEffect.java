package com.emoniph.witchery.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Source {@code PotionIllFitting}: ejects one armour piece near the end of the curse. */
public final class IllFittingMobEffect extends MobEffect {
    private static final EquipmentSlot[] ARMOUR = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    public IllFittingMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 15 == 0 && duration <= 15 * (Math.clamp(amplifier, 0, 3) + 1);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        EquipmentSlot slot = ARMOUR[level.random.nextInt(ARMOUR.length)];
        ItemStack armour = entity.getItemBySlot(slot);
        if (!armour.isEmpty()) {
            entity.setItemSlot(slot, ItemStack.EMPTY);
            entity.spawnAtLocation(level, armour);
        }
        return true;
    }
}
