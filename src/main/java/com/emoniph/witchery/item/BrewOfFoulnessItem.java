package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Brew of Foulness — nauseates and poisons all nearby living entities. */
public class BrewOfFoulnessItem extends BrewItem {

    public BrewOfFoulnessItem(Properties properties) { super(properties); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            int tier   = stack.getOrDefault(WitcheryDataComponents.BREW_POWER.get(), 1);
            double r   = 2 + tier * 2.0;       // tier1=4, tier3=8, tier5=12
            int nausea = BrewType.scale(300, tier);
            int poison = BrewType.scale(200, tier);
            AABB area  = new AABB(player.blockPosition()).inflate(r);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player);
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA,  nausea, 0));
                target.addEffect(new MobEffectInstance(MobEffects.POISON,  poison, 1));
            }
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0));
        }
        stack.shrink(1);
        if (entity instanceof Player player && !player.isCreative()) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
        }
        return stack;
    }
}
