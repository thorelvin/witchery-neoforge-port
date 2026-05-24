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

/** Brew of Poison — poisons and weakens all nearby living entities. */
public class BrewOfPoisonItem extends BrewItem {

    public BrewOfPoisonItem(Properties properties) { super(properties); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            int tier     = stack.getOrDefault(WitcheryDataComponents.BREW_POWER.get(), 1);
            double r     = 1 + tier;               // tier1=2, tier3=4, tier5=6
            int poison   = BrewType.scale(300, tier);
            int weakness = BrewType.scale(200, tier);
            AABB area    = new AABB(player.blockPosition()).inflate(r);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON,   poison,   2));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakness, 1));
            }
        }
        stack.shrink(1);
        if (entity instanceof Player player && !player.isCreative()) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
        }
        return stack;
    }
}
