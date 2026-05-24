package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Brew of Foaming — covers the ground in cobwebs around the drinker. */
public class BrewOfFoamingItem extends BrewItem {

    public BrewOfFoamingItem(Properties properties) { super(properties); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            int tier     = stack.getOrDefault(WitcheryDataComponents.BREW_POWER.get(), 1);
            int radius   = 1 + tier;              // tier1=2, tier3=4, tier5=6
            int radiusSq = radius * (radius + 1); // tier1=6, tier3=20, tier5=42
            BlockPos center = player.blockPosition();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radiusSq) continue;
                    BlockPos floor = center.offset(dx, 0, dz);
                    if (level.getBlockState(floor).isAir()) {
                        level.setBlockAndUpdate(floor, Blocks.COBWEB.defaultBlockState());
                    }
                }
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
